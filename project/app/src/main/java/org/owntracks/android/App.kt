package org.owntracks.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.StrictMode
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.work.Configuration
import androidx.work.InitializationExceptionHandler
import androidx.work.WorkerFactory
import dagger.hilt.EntryPoints
import dagger.hilt.android.HiltAndroidApp
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.conscrypt.Conscrypt
import org.owntracks.android.data.waypoints.RoomWaypointsRepo
import org.owntracks.android.di.CustomBindingComponentBuilder
import org.owntracks.android.di.CustomBindingEntryPoint
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.logging.TimberInMemoryLogTree
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.PreferencesStore
import org.owntracks.android.preferences.types.AppTheme
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.IdlingResourceWithData
import org.owntracks.android.support.SimpleIdlingResource
import timber.log.Timber
import java.security.Security
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

@HiltAndroidApp
class App : Application(), Configuration.Provider, Preferences.OnPreferenceChangeListener {
    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var workerFactory: WorkerFactory

    @Inject
    lateinit var scheduler: Scheduler

    @Inject
    lateinit var bindingComponentProvider: Provider<CustomBindingComponentBuilder>

    @Inject
    lateinit var messageProcessor: MessageProcessor

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    @Inject
    lateinit var preferencesStore: PreferencesStore

    @Inject
    @get:VisibleForTesting
    @Named("mockLocationIdlingResource")
    lateinit var mockLocationIdlingResource: SimpleIdlingResource

    @get:VisibleForTesting
    val preferenceSetIdlingResource: SimpleIdlingResource = SimpleIdlingResource("preferenceSetIdlingResource", true)

    @Inject
    @Named("outgoingQueueIdlingResource")
    @get:VisibleForTesting
    lateinit var outgoingQueueIdlingResource: CountingIdlingResource

    @Inject
    @Named("contactsClearedIdlingResource")
    @get:VisibleForTesting
    lateinit var contactsClearedIdlingResource: SimpleIdlingResource

    @Inject
    @Named("messageReceivedIdlingResource")
    @get:VisibleForTesting
    lateinit var messageReceivedIdlingResource: IdlingResourceWithData<MessageBase>

    @Inject
    lateinit var waypointsRepo: RoomWaypointsRepo

    val workManagerFailedToInitialize = MutableLiveData(false)

    @get:VisibleForTesting
    val migrationIdlingResource: SimpleIdlingResource = SimpleIdlingResource("waypointsMigration", false)

    override fun onCreate() {
        // Make sure we use Conscrypt for advanced TLS features on all devices.
        Security.insertProviderAt(
            Conscrypt.newProviderBuilder().provideTrustManager(true).build(),
            1
        )

        // Bring in a real version of BC and don't use the device version.
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())

        super.onCreate()

        val dataBindingComponent = bindingComponentProvider.get().build()
        val dataBindingEntryPoint = EntryPoints.get(
            dataBindingComponent,
            CustomBindingEntryPoint::class.java
        )

        DataBindingUtil.setDefaultComponent(dataBindingEntryPoint)

        scheduler.cancelAllTasks()
        Timber.plant(TimberInMemoryLogTree(BuildConfig.DEBUG))

        if (BuildConfig.DEBUG) {
            System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "debug")
            org.slf4j.LoggerFactory.getLogger(this::class.java).trace("SLF4J logging at trace level")
            Timber.e("StrictMode enabled in DEBUG build")
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectNetwork().penaltyFlashScreen().penaltyDialog().build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectFileUriExposure()
                    .penaltyLog()
                    .build()
            )
        }

        preferences.registerOnPreferenceChangedListener(this)

        setThemeFromPreferences()

        migrateWaypoints()

        // Notifications can be sent from multiple places, so let's make sure we've got the channels in place
        createNotificationChannels()
    }

    private fun setThemeFromPreferences() {
        when (preferences.theme) {
            AppTheme.AUTO -> AppCompatDelegate.setDefaultNightMode(
                Preferences.SYSTEM_NIGHT_AUTO_MODE
            )

            AppTheme.DARK -> AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES
            )

            AppTheme.LIGHT -> AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Importance min will show normal priority notification for foreground service. See https://developer.android.com/reference/android/app/NotificationManager#IMPORTANCE_MIN
            // User has to actively configure this in the notification channel settings.
            val ongoingNotificationChannelName =
                if (getString(R.string.notificationChannelOngoing).trim().isNotEmpty()) {
                    getString(R.string.notificationChannelOngoing)
                } else {
                    "Ongoing"
                }
            NotificationChannel(
                NOTIFICATION_CHANNEL_ONGOING,
                ongoingNotificationChannelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = getString(R.string.notificationChannelOngoingDescription)
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
                setSound(null, null)
            }.run { notificationManager.createNotificationChannel(this) }

            val eventsNotificationChannelName = if (getString(R.string.events).trim().isNotEmpty()) {
                getString(R.string.events)
            } else {
                "Events"
            }
            NotificationChannel(
                NOTIFICATION_CHANNEL_EVENTS,
                eventsNotificationChannelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = getString(R.string.notificationChannelEventsDescription)
                enableLights(false)
                enableVibration(false)
                setShowBadge(true)
                setSound(null, null)
            }.run { notificationManager.createNotificationChannel(this) }

            val errorNotificationChannelName = if (getString(R.string.notificationChannelErrors).trim().isNotEmpty()) {
                getString(R.string.notificationChannelErrors)
            } else {
                "Errors"
            }
            NotificationChannel(
                GeocoderProvider.ERROR_NOTIFICATION_CHANNEL_ID,
                errorNotificationChannelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }.run { notificationManager.createNotificationChannel(this) }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder()
        .setWorkerFactory(workerFactory)
        .setInitializationExceptionHandler(
            InitializationExceptionHandler { throwable ->
                Timber.e(throwable, "Exception thrown when initializing WorkManager")
                workManagerFailedToInitialize.postValue(true)
            }
        )
        .build()

    override fun onPreferenceChanged(properties: Set<String>) {
        if (properties.contains(Preferences::theme.name)) {
            Timber.d("Theme changed. Setting theme to ${preferences.theme}")
            setThemeFromPreferences()
        }
        Timber.v("Idling preferenceSetIdlingResource because of $properties")
        preferenceSetIdlingResource.setIdleState(true)
    }

    @get:VisibleForTesting
    val mqttConnectionIdlingResource: IdlingResource
        get() = messageProcessor.mqttConnectionIdlingResource

    /**
     * Migrate preferences. Available to be called from espresso tests.
     *
     */
    @VisibleForTesting
    fun migratePreferences() {
        preferencesStore.migrate()
    }

    /**
     * Migrate waypoints. We need a way to call this from an espresso test after it's written the test files
     * so have this visible for testing so it can be called post-startup
     */
    @VisibleForTesting
    fun migrateWaypoints() {
        Timber.v("UnIdling migrationIdlingResource")
        migrationIdlingResource.setIdleState(false)
        waypointsRepo.migrateFromLegacyStorage().invokeOnCompletion { throwable ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                throwable?.run {
                    Timber.e(throwable, "Error migrating waypoints")
                    NotificationCompat.Builder(
                        applicationContext,
                        GeocoderProvider.ERROR_NOTIFICATION_CHANNEL_ID
                    )
                        .setContentTitle(getString(R.string.waypointMigrationErrorNotificationTitle))
                        .setContentText(getString(R.string.waypointMigrationErrorNotificationText))
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.ic_owntracks_80)
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .bigText(getString(R.string.waypointMigrationErrorNotificationText))
                        )
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setSilent(true)
                        .build()
                        .run {
                            notificationManager.notify("WaypointsMigrationNotification", 0, this)
                        }
                }
            } else if (throwable != null ) {
                Timber.w(throwable,"notification permissions not granted, can't display waypoints migration error notification")
            }
            Timber.v("Idling migrationIdlingResource")
            migrationIdlingResource.setIdleState(true)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ONGOING = "O"
        const val NOTIFICATION_CHANNEL_EVENTS = "E"
        const val NOTIFICATION_ID_ONGOING = 1
        const val NOTIFICATION_ID_EVENT_GROUP = 2
        const val NOTIFICATION_GROUP_EVENTS = "events"
    }
}
