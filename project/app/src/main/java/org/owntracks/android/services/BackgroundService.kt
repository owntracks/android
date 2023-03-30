package org.owntracks.android.services

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Location
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.owntracks.android.App
import org.owntracks.android.R
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.data.waypoints.WaypointsRepo.WaypointAndOperation
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.location.*
import org.owntracks.android.location.geofencing.Geofence
import org.owntracks.android.location.geofencing.GeofencingClient
import org.owntracks.android.location.geofencing.GeofencingEvent
import org.owntracks.android.location.geofencing.GeofencingEvent.Companion.fromIntent
import org.owntracks.android.location.geofencing.GeofencingRequest
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageLocation.Companion.fromLocation
import org.owntracks.android.model.messages.MessageTransition
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.preferences.types.MonitoringMode.Companion.getByValue
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.DateFormatter.formatDate
import org.owntracks.android.support.RunThingsOnOtherThreads
import org.owntracks.android.support.ServiceBridge
import org.owntracks.android.support.ServiceBridge.ServiceBridgeInterface
import org.owntracks.android.support.SimpleIdlingResource
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber

@AndroidEntryPoint
class BackgroundService :
    LifecycleService(),
    ServiceBridgeInterface,
    Preferences.OnPreferenceChangeListener {
    private var locationCallback: LocationCallback? = null
    private var locationCallbackOnDemand: LocationCallback? = null
    private var lastLocationMessage: MessageLocation? = null
    private var activeNotificationCompatBuilder: NotificationCompat.Builder? = null
    private var eventsNotificationCompatBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private var notificationManagerCompat: NotificationManagerCompat? = null
    private val activeNotifications = LinkedList<Spannable>()
    private var lastQueueLength = 0
    private var hasBeenStartedExplicitly = false

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var scheduler: Scheduler

    @Inject
    lateinit var locationProcessor: LocationProcessor

    @Inject
    lateinit var geocoderProvider: GeocoderProvider

    @Inject
    lateinit var contactsRepo: ContactsRepo

    @Inject
    lateinit var locationRepo: LocationRepo

    @Inject
    lateinit var runThingsOnOtherThreads: RunThingsOnOtherThreads

    @Inject
    lateinit var waypointsRepo: WaypointsRepo

    @Inject
    lateinit var serviceBridge: ServiceBridge

    @Inject
    lateinit var messageProcessor: MessageProcessor

    @Inject
    lateinit var endpointStateRepo: EndpointStateRepo

    @Inject
    lateinit var geofencingClient: GeofencingClient

    @Inject
    lateinit var locationProviderClient: LocationProviderClient

    @Inject
    lateinit var locationIdlingResource: SimpleIdlingResource

    @Inject
    @CoroutineScopes.IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onCreate() {
        super.onCreate()
        serviceBridge.bind(this)
        notificationManagerCompat = NotificationManagerCompat.from(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        locationCallback = object : LocationCallback {
            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                Timber.d("location availability %s", locationAvailability)
            }

            override fun onLocationResult(locationResult: LocationResult) {
                Timber.d("location result received: %s", locationResult)
                Timber.v("Idling location")
                locationIdlingResource.setIdleState(true)
                onLocationChanged(locationResult.lastLocation, MessageLocation.REPORT_TYPE_DEFAULT)
            }
        }
        locationCallbackOnDemand = object : LocationCallback {
            override fun onLocationAvailability(locationAvailability: LocationAvailability) {}
            override fun onLocationResult(locationResult: LocationResult) {
                Timber.d("BackgroundService On-demand location result received: %s", locationResult)
                onLocationChanged(locationResult.lastLocation, MessageLocation.REPORT_TYPE_RESPONSE)
            }
        }
        startForeground(NOTIFICATION_ID_ONGOING, ongoingNotification)
        setupLocationRequest()
        scheduler.scheduleLocationPing()
        messageProcessor.initialize()
        preferences.registerOnPreferenceChangedListener(this)
        endpointStateRepo.endpointQueueLength.observe(this) { queueLength: Int ->
            lastQueueLength = queueLength
            updateOngoingNotification()
        }
        endpointStateRepo.endpointStateLiveData.observe(this) { updateOngoingNotification() }
        endpointStateRepo.setServiceStartedNow()

        // Every time a waypoint is inserted, updated or deleted, we need to update the geofences, and maybe publish that
        // waypoint
        waypointsRepo.operations.observe(this) { (operation, waypoint): WaypointAndOperation ->
            when (operation) {
                WaypointsRepo.Operation.INSERT,
                WaypointsRepo.Operation.UPDATE -> locationProcessor.publishWaypointMessage(waypoint)
                else -> {}
            }
            lifecycleScope.launch {
                setupGeofences()
            }
        }
        lifecycleScope.launch {
            setupGeofences()
        }

        locationRepo.currentPublishedLocation.observe(this) { location: Location ->
            val messageLocation = fromLocation(
                location,
                Build.VERSION.SDK_INT
            )
            if (lastLocationMessage == null || lastLocationMessage!!.timestamp < messageLocation.timestamp) {
                lastLocationMessage = messageLocation
                geocoderProvider.resolve(messageLocation, this)
            }
        }
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        preferences.unregisterOnPreferenceChangedListener(this)
        messageProcessor.stopSendingMessages()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action != null) {
            Timber.v("intent received with action:%s", intent.action)
            when (intent.action) {
                INTENT_ACTION_SEND_LOCATION_USER -> {
                    lifecycleScope.launch {
                        locationProcessor.publishLocationMessage(
                            MessageLocation.REPORT_TYPE_USER,
                            locationRepo.currentPublishedLocation.value
                        )
                    }
                    return
                }
                INTENT_ACTION_SEND_EVENT_CIRCULAR -> {
                    lifecycleScope.launch {
                        onGeofencingEvent(fromIntent(intent))
                    }
                    return
                }
                INTENT_ACTION_CLEAR_NOTIFICATIONS -> {
                    clearEventStackNotification()
                    return
                }
                INTENT_ACTION_REREQUEST_LOCATION_UPDATES -> {
                    setupLocationRequest()
                    return
                }
                INTENT_ACTION_CHANGE_MONITORING -> {
                    if (intent.hasExtra("monitoring")) {
                        val newMode = getByValue(
                            intent.getIntExtra(
                                "monitoring",
                                preferences.monitoring.value
                            )
                        )
                        preferences.monitoring = newMode
                    } else {
                        // Step monitoring mode if no mode is specified
                        preferences.setMonitoringNext()
                    }
                    hasBeenStartedExplicitly = true
                    notificationManager!!.cancel(
                        BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG,
                        0
                    )
                    return
                }
                INTENT_ACTION_BOOT_COMPLETED, INTENT_ACTION_PACKAGE_REPLACED -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                        !hasBeenStartedExplicitly && ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) == PackageManager.PERMISSION_DENIED
                    ) {
                        notifyUserOfBackgroundLocationRestriction()
                    }
                    return
                }
                INTENT_ACTION_EXIT -> {
                    exit()
                    return
                }
                else -> Timber.v("unhandled intent action received: %s", intent.action)
            }
        } else {
            hasBeenStartedExplicitly = true
        }
    }

    private fun exit() {
        stopSelf()
        scheduler.cancelAllTasks()
        Process.killProcess(Process.myPid())
    }

    private fun notifyUserOfBackgroundLocationRestriction() {
        val activityLaunchIntent = Intent(applicationContext, MapActivity::class.java)
        activityLaunchIntent.action = "android.intent.action.MAIN"
        activityLaunchIntent.addCategory("android.intent.category.LAUNCHER")
        activityLaunchIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val notificationText = getString(R.string.backgroundLocationRestrictionNotificationText)
        val notificationTitle = getString(R.string.backgroundLocationRestrictionNotificationTitle)
        val notification = NotificationCompat.Builder(
            applicationContext,
            GeocoderProvider.ERROR_NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_owntracks_80)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationText)
            )
            .setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    activityLaunchIntent,
                    updateCurrentIntentFlags
                )
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
        notificationManager!!.notify(
            BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG,
            0,
            notification
        )
    }

    private val ongoingNotificationBuilder: NotificationCompat.Builder?
        get() {
            if (activeNotificationCompatBuilder != null) return activeNotificationCompatBuilder
            val resultIntent = Intent(this, MapActivity::class.java)
            resultIntent.action = "android.intent.action.MAIN"
            resultIntent.addCategory("android.intent.category.LAUNCHER")
            resultIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            val resultPendingIntent =
                PendingIntent.getActivity(this, 0, resultIntent, updateCurrentIntentFlags)
            val publishIntent = Intent()
            publishIntent.action = INTENT_ACTION_SEND_LOCATION_USER
            val publishPendingIntent =
                PendingIntent.getService(this, 0, publishIntent, updateCurrentIntentFlags)
            publishIntent.action = INTENT_ACTION_CHANGE_MONITORING
            val changeMonitoringPendingIntent =
                PendingIntent.getService(this, 0, publishIntent, updateCurrentIntentFlags)
            activeNotificationCompatBuilder =
                NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_ONGOING)
                    .setContentIntent(resultPendingIntent)
                    .setSortKey("a")
                    .addAction(
                        R.drawable.ic_baseline_publish_24,
                        getString(R.string.publish),
                        publishPendingIntent
                    )
                    .addAction(
                        R.drawable.ic_owntracks_80,
                        getString(R.string.notificationChangeMonitoring),
                        changeMonitoringPendingIntent
                    )
                    .setSmallIcon(R.drawable.ic_owntracks_80)
                    .setPriority(
                        if (preferences.notificationHigherPriority) {
                            NotificationCompat.PRIORITY_DEFAULT
                        } else {
                            NotificationCompat.PRIORITY_MIN
                        }
                    )
                    .setSound(null, AudioManager.STREAM_NOTIFICATION)
                    .setOngoing(true)
            activeNotificationCompatBuilder!!.setColor(getColor(com.mikepenz.materialize.R.color.primary))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            return activeNotificationCompatBuilder
        }

    private fun updateOngoingNotification() {
        notificationManager!!.notify(NOTIFICATION_ID_ONGOING, ongoingNotification)
    }

    // Show monitoring mode if endpoint state is not interesting
    private val ongoingNotification: Notification?
        get() {
            val builder = ongoingNotificationBuilder ?: return null
            if (lastLocationMessage != null && preferences.notificationLocation) {
                builder.setContentTitle(lastLocationMessage!!.geocode)
                builder.setWhen(TimeUnit.SECONDS.toMillis(lastLocationMessage!!.timestamp))
                builder.setNumber(lastQueueLength)
            } else {
                builder.setContentTitle(getString(R.string.app_name))
            }

            // Show monitoring mode if endpoint state is not interesting
            val lastEndpointState = endpointStateRepo.endpointStateLiveData.value
            if (lastEndpointState === EndpointState.CONNECTED || lastEndpointState === EndpointState.IDLE) {
                builder.setContentText(getMonitoringLabel(preferences.monitoring))
            } else if (lastEndpointState === EndpointState.ERROR && lastEndpointState.message != null) {
                builder.setContentText(lastEndpointState.getLabel(this) + ": " + lastEndpointState.message)
            } else {
                builder.setContentText(lastEndpointState!!.getLabel(this))
            }
            return builder.build()
        }

    private fun getMonitoringLabel(mode: MonitoringMode): String {
        return when (mode) {
            MonitoringMode.QUIET -> getString(R.string.monitoring_quiet)
            MonitoringMode.MANUAL -> getString(R.string.monitoring_manual)
            MonitoringMode.SIGNIFICANT -> getString(R.string.monitoring_significant)
            MonitoringMode.MOVE -> getString(R.string.monitoring_move)
        }
    }

    override fun sendEventNotification(message: MessageTransition) {
        val builder = eventsNotificationBuilder
        if (builder == null) {
            Timber.e("no builder returned")
            return
        }
        val c = contactsRepo.getById(message.contactKey)
        val timestampInMs = TimeUnit.SECONDS.toMillis(message.timestamp)
        var location = message.description
        if (location == null) {
            location = getString(R.string.aLocation)
        }
        var title = message.trackerId
        if (c != null) {
            title = c.fusedName
        } else if (title == null) {
            title = message.contactKey
        }
        val text = String.format(
            "%s %s",
            getString(
                if (message.getTransition() == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    R.string.transitionEntering
                } else {
                    R.string.transitionLeaving
                }
            ),
            location
        )
        eventsNotificationCompatBuilder!!.setContentTitle(title)
        eventsNotificationCompatBuilder!!.setContentText(text)
        eventsNotificationCompatBuilder!!.setWhen(TimeUnit.SECONDS.toMillis(message.timestamp))
        eventsNotificationCompatBuilder!!.setShowWhen(true)
        eventsNotificationCompatBuilder!!.setGroup(NOTIFICATION_GROUP_EVENTS)

        // Deliver notification
        val n = eventsNotificationCompatBuilder!!.build()
        sendEventStackNotification(title, text, Date(timestampInMs))
    }

    private fun sendEventStackNotification(title: String, text: String, timestamp: Date) {
        Timber.v("SDK_INT >= 23, building stack notification")
        val whenStr = formatDate(timestamp)
        val newLine: Spannable = SpannableString(String.format("%s %s %s", whenStr, title, text))
        newLine.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            whenStr.length + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        activeNotifications.push(newLine)
        Timber.v("groupedNotifications: %s", activeNotifications.size)
        val summary = resources.getQuantityString(
            R.plurals.notificationEventsTitle,
            activeNotifications.size,
            activeNotifications.size
        )
        val inbox = NotificationCompat.InboxStyle()
        inbox.setSummaryText(summary)
        for (n in activeNotifications) {
            inbox.addLine(n)
        }
        val builder = NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_EVENTS)
            .setContentTitle(getString(R.string.events))
            .setContentText(summary)
            .setGroup(NOTIFICATION_GROUP_EVENTS) // same as group of single notifications
            .setGroupSummary(true)
            .setColor(getColor(com.mikepenz.materialize.R.color.primary))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.drawable.ic_owntracks_80)
            .setLocalOnly(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setNumber(activeNotifications.size)
            .setStyle(inbox)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    System.currentTimeMillis()
                        .toInt() / 1000,
                    Intent(this, MapActivity::class.java),
                    updateCurrentIntentFlags
                )
            )
            .setDeleteIntent(
                PendingIntent.getService(
                    this,
                    INTENT_REQUEST_CODE_CLEAR_EVENTS,
                    Intent(this, BackgroundService::class.java).setAction(
                        INTENT_ACTION_CLEAR_NOTIFICATIONS
                    ),
                    updateCurrentIntentFlags
                )
            )
        val stackNotification = builder.build()
        notificationManagerCompat!!.notify(
            NOTIFICATION_GROUP_EVENTS,
            NOTIFICATION_ID_EVENT_GROUP,
            stackNotification
        )
    }

    private fun clearEventStackNotification() {
        Timber.v("clearing notification stack")
        activeNotifications.clear()
    }

    private suspend fun onGeofencingEvent(event: GeofencingEvent) {
        if (event.hasError()) {
            Timber.e("geofencingEvent hasError: ${event.errorCode}")
            return
        }
        if (event.geofenceTransition == null || event.triggeringGeofences == null || event.triggeringLocation == null) {
            Timber.e("geofencingEvent has no transition or trigger")
            return
        }
        val transition: Int = event.geofenceTransition
        event.triggeringGeofences.forEach { triggeringGeofence ->
            val requestId = triggeringGeofence.requestId
            if (requestId != null) {
                try {
                    waypointsRepo.get(requestId.toLong())
                        ?.run {
                            locationProcessor.onWaypointTransition(
                                this,
                                event.triggeringLocation,
                                transition,
                                MessageTransition.TRIGGER_CIRCULAR
                            )
                        } ?: run {
                        Timber.e("waypoint id $requestId not found for geofence event")
                    }
                } catch (e: NumberFormatException) {
                    Timber.e("$requestId from Geofencing event is not a valid request id")
                }
            }
        }
    }

    fun onLocationChanged(location: Location, reportType: String) {
        Timber.v("location update received: $location, report type $reportType")
        if (location.time > locationRepo.currentLocationTime) {
            lifecycleScope.launch {
                locationProcessor.onLocationChanged(location, reportType)
            }
        } else {
            Timber.v("Not re-sending message with same timestamp as last")
        }
    }

    override fun requestOnDemandLocationUpdate() {
        if (missingLocationPermission()) {
            Timber.e("missing location permission")
            return
        }
        val request = LocationRequest(
            null,
            null,
            1,
            Duration.ofMinutes(1),
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            Duration.ofMinutes(1),
            null
        )
        Timber.d("On demand location request")
        locationProviderClient.requestLocationUpdates(
            request,
            locationCallbackOnDemand!!,
            runThingsOnOtherThreads.getBackgroundLooper()
        )
    }

    private fun setupLocationRequest(): Boolean {
        Timber.v("setupLocationRequest")
        if (missingLocationPermission()) {
            Timber.e("missing location permission")
            return false
        }

        val monitoring = preferences.monitoring
        var interval: Duration? = null
        var fastestInterval: Duration? = null
        var smallestDisplacement: Float? = null
        var priority: Int? = null
        when (monitoring) {
            MonitoringMode.QUIET, MonitoringMode.MANUAL -> {
                interval = Duration.ofSeconds(preferences.locatorInterval.toLong())
                smallestDisplacement = preferences.locatorDisplacement.toFloat()
                priority = LocationRequest.PRIORITY_LOW_POWER
            }
            MonitoringMode.SIGNIFICANT -> {
                interval = Duration.ofSeconds(preferences.locatorInterval.toLong())
                smallestDisplacement = preferences.locatorDisplacement.toFloat()
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            }
            MonitoringMode.MOVE -> {
                interval = Duration.ofSeconds(preferences.moveModeLocatorInterval.toLong())
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
        }
        if (preferences.pegLocatorFastestIntervalToInterval) {
            fastestInterval = interval
        }
        val request = LocationRequest(
            fastestInterval,
            smallestDisplacement,
            null,
            null,
            priority,
            interval,
            null
        )
        Timber.d("location update request params: %s", request)
        locationProviderClient.flushLocations()
        locationProviderClient.requestLocationUpdates(
            request,
            locationCallback!!,
            runThingsOnOtherThreads.getBackgroundLooper()
        )
        return true
    }

    private suspend fun setupGeofences() {
        if (missingLocationPermission()) {
            Timber.e("missing location permission")
            return
        }
        withContext(ioDispatcher) {
            val waypoints = waypointsRepo.all

            Timber.i("Setting up geofences for ${waypoints.size} waypoints")
            val geofences = LinkedList<Geofence>()
            for ((id, description, geofenceLatitude, geofenceLongitude, geofenceRadius) in waypoints) {
                Timber.d(
                    "id:%s, desc:%s, lat:%s, lon:%s, rad:%s",
                    id,
                    description,
                    geofenceLatitude,
                    geofenceLongitude,
                    geofenceRadius
                )
                try {
                    geofences.add(
                        Geofence(
                            id.toString(),
                            Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT,
                            TimeUnit.MINUTES.toMillis(2)
                                .toInt(),
                            geofenceLatitude,
                            geofenceLongitude,
                            geofenceRadius.toFloat(),
                            Geofence.NEVER_EXPIRE,
                            null
                        )
                    )
                } catch (e: IllegalArgumentException) {
                    Timber.e(e, "Invalid geofence parameter")
                }
            }
            geofencingClient.removeGeofences(this@BackgroundService)
            if (geofences.size > 0) {
                val request = GeofencingRequest(Geofence.GEOFENCE_TRANSITION_ENTER, geofences)
                geofencingClient.addGeofences(request, this@BackgroundService)
            }
        }
    }

    private fun missingLocationPermission(): Boolean {
        return (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_DENIED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_DENIED
            )
    }

    fun onGeocodingProviderResult(m: MessageLocation) {
        if (m === lastLocationMessage) {
            updateOngoingNotification()
        }
    }

    private val eventsNotificationBuilder: NotificationCompat.Builder?
        get() {
            if (!preferences.notificationEvents) return null
            if (eventsNotificationCompatBuilder != null) return eventsNotificationCompatBuilder
            val openIntent = Intent(this, MapActivity::class.java)
            openIntent.action = "android.intent.action.MAIN"
            openIntent.addCategory("android.intent.category.LAUNCHER")
            openIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            val openPendingIntent =
                PendingIntent.getActivity(this, 0, openIntent, updateCurrentIntentFlags)
            eventsNotificationCompatBuilder =
                NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_EVENTS)
                    .setContentIntent(openPendingIntent)
                    .setSmallIcon(R.drawable.ic_baseline_add_24)
                    .setAutoCancel(true)
                    .setShowWhen(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            eventsNotificationCompatBuilder!!.color =
                getColor(com.mikepenz.materialize.R.color.primary)
            return eventsNotificationCompatBuilder
        }

    override fun onPreferenceChanged(properties: Set<String>) {
        val propertiesWeCareAbout = listOf(
            "locatorInterval",
            "locatorDisplacement",
            "moveModeLocatorInterval",
            "pegLocatorFastestIntervalToInterval"
        )
        if (!propertiesWeCareAbout.stream()
                .filter { o: String -> properties.contains(o) }
                .collect(
                    Collectors.toSet()
                )
                .isEmpty()
        ) {
            Timber.d("locator preferences changed. Resetting location request.")
            setupLocationRequest()
        }
        if (properties.contains("monitoring")) {
            setupLocationRequest()
            updateOngoingNotification()
        }
    }

    fun reInitializeLocationRequests() {
        runThingsOnOtherThreads.postOnServiceHandlerDelayed({
            if (setupLocationRequest()) {
                Timber.d("Getting last location")
                val lastLocation = locationProviderClient.getLastLocation()
                if (lastLocation != null) {
                    onLocationChanged(lastLocation, MessageLocation.REPORT_TYPE_DEFAULT)
                }
            }
        }, 0)
    }

    private val localServiceBinder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: BackgroundService
            get() = this@BackgroundService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Timber.d("Background service bound")
        return localServiceBinder
    }

    companion object {
        private const val INTENT_REQUEST_CODE_CLEAR_EVENTS = 1263
        private const val NOTIFICATION_ID_ONGOING = 1
        private const val NOTIFICATION_ID_EVENT_GROUP = 2
        const val BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG =
            "backgroundRestrictionNotification"

        private const val NOTIFICATION_GROUP_EVENTS = "events"

        // NEW ACTIONS ALSO HAVE TO BE ADDED TO THE SERVICE INTENT FILTER
        private const val INTENT_ACTION_CLEAR_NOTIFICATIONS =
            "org.owntracks.android.CLEAR_NOTIFICATIONS"
        private const val INTENT_ACTION_SEND_LOCATION_USER =
            "org.owntracks.android.SEND_LOCATION_USER"
        const val INTENT_ACTION_SEND_EVENT_CIRCULAR = "org.owntracks.android.SEND_EVENT_CIRCULAR"
        const val INTENT_ACTION_REREQUEST_LOCATION_UPDATES =
            "org.owntracks.android.REREQUEST_LOCATION_UPDATES"
        private const val INTENT_ACTION_CHANGE_MONITORING =
            "org.owntracks.android.CHANGE_MONITORING"
        private const val INTENT_ACTION_EXIT = "org.owntracks.android.EXIT"
        private const val INTENT_ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
        private const val INTENT_ACTION_PACKAGE_REPLACED =
            "android.intent.action.MY_PACKAGE_REPLACED"
        private const val updateCurrentIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    }
}
