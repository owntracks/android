package org.owntracks.android.services

import android.Manifest
import android.app.Notification
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
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
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.location.LocationAvailability
import org.owntracks.android.location.LocationCallback
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.location.LocationRequest
import org.owntracks.android.location.LocationResult
import org.owntracks.android.location.geofencing.Geofence
import org.owntracks.android.location.geofencing.GeofencingClient
import org.owntracks.android.location.geofencing.GeofencingEvent
import org.owntracks.android.location.geofencing.GeofencingEvent.Companion.fromIntent
import org.owntracks.android.location.geofencing.GeofencingRequest
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude
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
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber

@AndroidEntryPoint
class BackgroundService :
    LifecycleService(), ServiceBridgeInterface, Preferences.OnPreferenceChangeListener {
    private var lastLocationMessage: MessageLocation? = null

    private lateinit var notificationManagerCompat: NotificationManagerCompat
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
    @CoroutineScopes.IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val callbackForReportType =
        mutableMapOf<MessageLocation.ReportType, Lazy<LocationCallbackWithReportType>>().apply {
            MessageLocation.ReportType.values().forEach {
                this[it] = lazy {
                    LocationCallbackWithReportType(it, locationProcessor, lifecycleScope)
                }
            }
        }

    // Active notification intents and builder
    private val resultIntent by lazy {
        Intent(this, MapActivity::class.java)
            .setAction("android.intent.action.MAIN")
            .addCategory("android.intent.category.LAUNCHER")
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    private val resultPendingIntent by lazy {
        PendingIntent.getActivity(this, 0, resultIntent, updateCurrentIntentFlags)
    }
    private val publishPendingIntent by lazy {
        PendingIntent.getService(
            this,
            0,
            Intent().setAction(INTENT_ACTION_SEND_LOCATION_USER),
            updateCurrentIntentFlags
        )
    }
    private val changeMonitoringPendingIntent by lazy {
        PendingIntent.getService(
            this,
            0,
            Intent().setAction(INTENT_ACTION_CHANGE_MONITORING),
            updateCurrentIntentFlags
        )
    }
    private val activeNotificationCompatBuilder: NotificationCompat.Builder by lazy {
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
            .setColor(getColor(R.color.OTPrimaryBlue))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface ServiceEntrypoint {
        fun preferences(): Preferences

        fun endpointStateRepo(): EndpointStateRepo
    }

    override fun onCreate() {
        Timber.d("Backgroundservice onCreate")
        val entrypoint = EntryPoints.get(applicationContext, ServiceEntrypoint::class.java)
        preferences = entrypoint.preferences()
        endpointStateRepo = entrypoint.endpointStateRepo()
        Timber.d("BackgroundService has injected. calling startForeground")
        startForeground(NOTIFICATION_ID_ONGOING, ongoingNotification)
        Timber.d("BackgroundService super.OnCreate")
        super.onCreate()
        serviceBridge.bind(this)
        notificationManagerCompat = NotificationManagerCompat.from(this)

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

        lifecycleScope.launch {
            // Every time a waypoint is inserted, updated or deleted, we need to update the geofences, and
            // maybe publish that
            // waypoint
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    waypointsRepo.migrationCompleteFlow.collect {
                        if (it) {
                            waypointsRepo.operations.collect { waypointOperation ->
                                when (waypointOperation) {
                                    is WaypointsRepo.WaypointOperation.Insert ->
                                        locationProcessor.publishWaypointMessage(waypointOperation.waypoint)
                                    is WaypointsRepo.WaypointOperation.Update ->
                                        locationProcessor.publishWaypointMessage(waypointOperation.waypoint)
                                    else -> {}
                                }
                                lifecycleScope.launch { setupGeofences() }
                            }
                        }
                    }
                }
                launch {
                    locationRepo.currentPublishedLocation.collect { location ->
                        location?.run {
                            val messageLocation = fromLocation(location, Build.VERSION.SDK_INT)
                            if (lastLocationMessage == null ||
                                lastLocationMessage!!.timestamp < messageLocation.timestamp
                            ) {
                                lastLocationMessage = messageLocation
                                geocoderProvider.resolve(messageLocation, this@BackgroundService)
                            }
                        }
                    }
                }
            }
            setupGeofences()
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

    /**
     * We've been sent a start command with an intent, which usually means we've got to do something
     * depending on the action
     *
     * @param intent that was passed to the service start command
     */
    private fun handleIntent(intent: Intent) {
        if (intent.action != null) {
            Timber.v("intent received with action:${intent.action}")
            when (intent.action) {
                INTENT_ACTION_SEND_LOCATION_USER -> {
                    lifecycleScope.launch {
                        locationProcessor.publishLocationMessage(
                            MessageLocation.ReportType.USER,
                            locationRepo.currentPublishedLocation.value
                        )
                    }
                    return
                }
                // This comes from the [GeofencingBroadcastReceiver]
                INTENT_ACTION_SEND_EVENT_CIRCULAR -> {
                    lifecycleScope.launch { onGeofencingEvent(fromIntent(intent)) }
                    return
                }

                // Called when the events are cancelled
                INTENT_ACTION_CLEAR_NOTIFICATIONS -> {
                    clearEventStackNotification()
                    return
                }
                INTENT_ACTION_CHANGE_MONITORING -> {
                    if (intent.hasExtra("monitoring")) {
                        val newMode = getByValue(intent.getIntExtra("monitoring", preferences.monitoring.value))
                        preferences.monitoring = newMode
                    } else {
                        // Step monitoring mode if no mode is specified
                        preferences.setMonitoringNext()
                    }
                    hasBeenStartedExplicitly = true
                    notificationManagerCompat.cancel(BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG, 0)
                    return
                }
                INTENT_ACTION_BOOT_COMPLETED,
                INTENT_ACTION_PACKAGE_REPLACED -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val backgroundLocationPermissionDenied =
                            ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            ) ==
                                PackageManager.PERMISSION_DENIED
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                            !hasBeenStartedExplicitly &&
                            backgroundLocationPermissionDenied
                        ) {
                            notifyUserOfBackgroundLocationRestriction()
                        }
                    }

                    return
                }
                INTENT_ACTION_EXIT -> {
                    exit()
                    return
                }
                else -> {}
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val activityLaunchIntent =
            Intent(applicationContext, MapActivity::class.java)
                .setAction("android.intent.action.MAIN")
                .addCategory("android.intent.category.LAUNCHER")
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val notificationText = getString(R.string.backgroundLocationRestrictionNotificationText)
        val notificationTitle = getString(R.string.backgroundLocationRestrictionNotificationTitle)
        val notification =
            NotificationCompat.Builder(
                applicationContext,
                GeocoderProvider.ERROR_NOTIFICATION_CHANNEL_ID
            )
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_owntracks_80)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
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
        notificationManagerCompat.notify(
            BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG,
            0,
            notification
        )
    }

    private fun updateOngoingNotification() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationManagerCompat.notify(NOTIFICATION_ID_ONGOING, ongoingNotification)
        }
    }

    // Show monitoring mode if endpoint state is not interesting
    private val ongoingNotification: Notification
        get() =
            activeNotificationCompatBuilder
                .apply {
                    if (lastLocationMessage != null && preferences.notificationLocation) {
                        setContentTitle(lastLocationMessage!!.geocode)
                        setWhen(TimeUnit.SECONDS.toMillis(lastLocationMessage!!.timestamp))
                        setNumber(lastQueueLength)
                    } else {
                        setContentTitle(getString(R.string.app_name))
                    }
                    // Show monitoring mode if endpoint state is not interesting
                    val lastEndpointState = endpointStateRepo.endpointStateLiveData.value
                    if (lastEndpointState === EndpointState.CONNECTED ||
                        lastEndpointState === EndpointState.IDLE
                    ) {
                        setContentText(getMonitoringLabel(preferences.monitoring))
                    } else if (lastEndpointState === EndpointState.ERROR &&
                        lastEndpointState.message != null
                    ) {
                        setContentText(
                            lastEndpointState.getLabel(this@BackgroundService) +
                                ": " +
                                lastEndpointState.message
                        )
                    } else {
                        setContentText(lastEndpointState!!.getLabel(this@BackgroundService))
                    }
                }
                .build()

    private fun getMonitoringLabel(mode: MonitoringMode): String {
        return when (mode) {
            MonitoringMode.QUIET -> getString(R.string.monitoring_quiet)
            MonitoringMode.MANUAL -> getString(R.string.monitoring_manual)
            MonitoringMode.SIGNIFICANT -> getString(R.string.monitoring_significant)
            MonitoringMode.MOVE -> getString(R.string.monitoring_move)
        }
    }

    override fun sendEventNotification(message: MessageTransition) {
        Timber.d("Sending event notification for $message")
        if (!preferences.notificationEvents ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val contact = contactsRepo.getById(message.contactKey)
        val timestampInMs = TimeUnit.SECONDS.toMillis(message.timestamp)
        val location = message.description ?: getString(R.string.aLocation)
        val title = contact?.name ?: message.trackerId ?: message.contactKey
        val transitionText =
            getString(
                if (message.getTransition() == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    R.string.transitionEntering
                } else {
                    R.string.transitionLeaving
                }
            )
        val eventText = "$transitionText $location"
        val whenStr = formatDate(timestampInMs)
        activeNotifications.push(
            SpannableString("$whenStr $title $eventText").apply {
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    whenStr.length + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        )
        Timber.v("groupedNotifications: ${activeNotifications.size}")
        val summary =
            resources.getQuantityString(
                R.plurals.notificationEventsTitle,
                activeNotifications.size,
                activeNotifications.size
            )
        val inbox = NotificationCompat.InboxStyle().setSummaryText(summary)
        activeNotifications.forEach { inbox.addLine(it) }

        NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_EVENTS)
            .setContentTitle(getString(R.string.events))
            .setContentText(summary)
            .setGroup(NOTIFICATION_GROUP_EVENTS) // same as group of single notifications
            .setGroupSummary(true)
            .setColor(getColor(R.color.OTPrimaryBlue))
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
                    System.currentTimeMillis().toInt() / 1000,
                    Intent(this, MapActivity::class.java),
                    updateCurrentIntentFlags
                )
            )
            .setDeleteIntent(
                PendingIntent.getService(
                    this,
                    1,
                    Intent(this, BackgroundService::class.java)
                        .setAction(INTENT_ACTION_CLEAR_NOTIFICATIONS),
                    updateCurrentIntentFlags
                )
            )
            .build()
            .run {
                notificationManagerCompat.notify(
                    NOTIFICATION_GROUP_EVENTS,
                    NOTIFICATION_ID_EVENT_GROUP,
                    this
                )
            }
    }

    fun clearEventStackNotification() {
        Timber.v("clearing notification stack")
        activeNotifications.clear()
    }

    private suspend fun onGeofencingEvent(event: GeofencingEvent) {
        if (event.hasError()) {
            Timber.e("geofencingEvent hasError: ${event.errorCode}")
            return
        }
        if (event.geofenceTransition == null ||
            event.triggeringGeofences == null ||
            event.triggeringLocation == null
        ) {
            Timber.e("geofencingEvent has no transition or trigger")
            return
        }
        val transition: Int = event.geofenceTransition
        event.triggeringGeofences.forEach { triggeringGeofence ->
            val requestId = triggeringGeofence.requestId
            if (requestId != null) {
                try {
                    waypointsRepo.get(requestId.toLong())?.run {
                        Timber.d("onWaypointTransition triggered by geofencing event")
                        locationProcessor.onWaypointTransition(
                            this,
                            event.triggeringLocation,
                            transition,
                            MessageTransition.TRIGGER_CIRCULAR
                        )
                    } ?: run { Timber.e("waypoint id $requestId not found for geofence event") }
                } catch (e: NumberFormatException) {
                    Timber.e("$requestId from Geofencing event is not a valid request id")
                }
            }
        }
    }

    override fun requestOnDemandLocationUpdate(reportType: MessageLocation.ReportType) {
        if (locationPermissionIsMissing()) {
            Timber.e("missing location permission")
            return
        }
        Timber.d("On demand location request")
        locationProviderClient.singleHighAccuracyLocation(
            callbackForReportType[reportType]!!.value,
            runThingsOnOtherThreads.getBackgroundLooper()
        )
    }

    private fun setupLocationRequest(): Boolean {
        Timber.v("setupLocationRequest")
        if (locationPermissionIsMissing()) {
            Timber.e("missing location permission")
            return false
        }

        val monitoring = preferences.monitoring
        var interval: Duration? = null
        var fastestInterval: Duration? = null
        var smallestDisplacement: Float? = null
        var priority: Int? = null
        when (monitoring) {
            MonitoringMode.QUIET,
            MonitoringMode.MANUAL -> {
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
        val request =
            LocationRequest(fastestInterval, smallestDisplacement, null, null, priority, interval, null)
        Timber.d("location update request params: $request")
        locationProviderClient.flushLocations()
        locationProviderClient.requestLocationUpdates(
            request,
            callbackForReportType[MessageLocation.ReportType.DEFAULT]!!.value,
            runThingsOnOtherThreads.getBackgroundLooper()
        )
        return true
    }

    private suspend fun setupGeofences() {
        if (locationPermissionIsMissing()) {
            Timber.e("missing location permission")
            return
        }
        withContext(ioDispatcher) {
            val waypoints = waypointsRepo.all
            Timber.i("Setting up geofences for ${waypoints.size} waypoints")
            val geofences =
                waypoints
                    .map {
                        Geofence(
                            it.id.toString(),
                            Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT,
                            2.minutes.inWholeMilliseconds.toInt(),
                            Latitude(it.geofenceLatitude),
                            Longitude(it.geofenceLongitude),
                            it.geofenceRadius.toFloat(),
                            Geofence.NEVER_EXPIRE,
                            null
                        )
                    }
                    .toList()
            geofencingClient.removeGeofences(this@BackgroundService)
            if (geofences.isNotEmpty()) {
                val request = GeofencingRequest(Geofence.GEOFENCE_TRANSITION_ENTER, geofences)
                geofencingClient.addGeofences(request, this@BackgroundService)
            }
        }
    }

    private fun locationPermissionIsMissing(): Boolean {
        return (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_DENIED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_DENIED
            )
    }

    fun onGeocodingProviderResult(m: MessageLocation) {
        if (m === lastLocationMessage) {
            updateOngoingNotification()
        }
    }

    override fun onPreferenceChanged(properties: Set<String>) {
        val propertiesWeCareAbout =
            listOf(
                "locatorInterval",
                "locatorDisplacement",
                "moveModeLocatorInterval",
                "pegLocatorFastestIntervalToInterval"
            )
        if (propertiesWeCareAbout
            .stream()
            .filter { o: String -> properties.contains(o) }
            .collect(Collectors.toSet())
            .isNotEmpty()
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
        runThingsOnOtherThreads.postOnServiceHandlerDelayed(
            {
                if (setupLocationRequest()) {
                    Timber.d("Getting last location")
                    locationProviderClient.getLastLocation()?.run {
                        lifecycleScope.launch {
                            locationProcessor.onLocationChanged(
                                this@run,
                                MessageLocation.ReportType.DEFAULT
                            )
                        }
                    }
                }
            },
            0
        )
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
        private const val NOTIFICATION_ID_ONGOING = 1
        private const val NOTIFICATION_ID_EVENT_GROUP = 2
        const val BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG = "backgroundRestrictionNotification"

        private const val NOTIFICATION_GROUP_EVENTS = "events"

        // NEW ACTIONS ALSO HAVE TO BE ADDED TO THE SERVICE INTENT FILTER
        private const val INTENT_ACTION_SEND_LOCATION_USER = "org.owntracks.android.SEND_LOCATION_USER"
        const val INTENT_ACTION_SEND_EVENT_CIRCULAR = "org.owntracks.android.SEND_EVENT_CIRCULAR"
        private const val INTENT_ACTION_CLEAR_NOTIFICATIONS =
            "org.owntracks.android.CLEAR_EVENT_NOTIFICATIONS"
        private const val INTENT_ACTION_CHANGE_MONITORING = "org.owntracks.android.CHANGE_MONITORING"
        private const val INTENT_ACTION_EXIT = "org.owntracks.android.EXIT"
        private const val INTENT_ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
        private const val INTENT_ACTION_PACKAGE_REPLACED = "android.intent.action.MY_PACKAGE_REPLACED"
        private const val updateCurrentIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    }

    class LocationCallbackWithReportType(
        private val reportType: MessageLocation.ReportType,
        private val locationProcessor: LocationProcessor,
        private val lifecycleCoroutineScope: LifecycleCoroutineScope
    ) : LocationCallback {

        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            Timber.v("Location availability $locationAvailability")
        }

        override fun onLocationResult(locationResult: LocationResult) {
            Timber.d("Location result received: $locationResult")
            onLocationChanged(locationResult.lastLocation, reportType)
        }

        override fun onLocationError() {
            Timber.v("Callback fired with no location received")
        }

        private fun onLocationChanged(location: Location, reportType: MessageLocation.ReportType) {
            Timber.v("backgroundservice location update received: $location, report type $reportType")
            lifecycleCoroutineScope.launch { locationProcessor.onLocationChanged(location, reportType) }
        }

        override fun toString(): String {
            return "Backgroundservice callback[$reportType] "
        }
    }
}
