package org.owntracks.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.StrictMode
import androidx.core.app.NotificationManagerCompat
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import org.conscrypt.Conscrypt
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.injection.components.DaggerAppComponent
import org.owntracks.android.injection.qualifier.AppContext
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.Events.RestartApp
import org.owntracks.android.support.Preferences
import org.owntracks.android.support.RunThingsOnOtherThreads
import org.owntracks.android.support.TimberDebugLogTree
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.security.Security
import javax.inject.Inject
import javax.inject.Singleton

class App : DaggerApplication() {
    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var runThingsOnOtherThreads: RunThingsOnOtherThreads

    @Inject
    lateinit var messageProcessor: MessageProcessor

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var workerFactory: WorkerFactory

    @Inject
    lateinit var scheduler: Scheduler

    override fun onCreate() {
        // Make sure we use Conscrypt for advanced TLS features on all devices.
        // X509ExtendedTrustManager not available pre-24, fall back to device. https://github.com/google/conscrypt/issues/603
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Security.insertProviderAt(Conscrypt.newProviderBuilder().provideTrustManager(true).build(), 1)
        } else {
            Security.insertProviderAt(Conscrypt.newProviderBuilder().provideTrustManager(false).build(), 1)
        }

        super.onCreate()
        WorkManager.initialize(this, Configuration.Builder().setWorkerFactory(workerFactory).build())
        scheduler.cancelAllTasks()
        if (BuildConfig.DEBUG) {
            Timber.plant(TimberDebugLogTree())
            Timber.e("StrictMode enabled in DEBUG build")
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectNetwork()
                    .penaltyFlashScreen()
                    .penaltyDialog()
                    .build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectFileUriExposure()
                    .penaltyLog()
                    .build())
        } else {
            Timber.plant(DebugTree())
        }
        for (t in Timber.forest()) {
            Timber.v("Planted trees :%s", t)
        }
        preferences.checkFirstStart()

        // Running this on a background thread will deadlock FirebaseJobDispatcher.
        // Initialize will call Scheduler to connect off the main thread anyway.
        runThingsOnOtherThreads.postOnMainHandlerDelayed(Runnable { messageProcessor.initialize() }, 510)
        eventBus.register(this)

        // Notifications can be sent from multiple places, so let's make sure we've got the channels in place
        createNotificationChannels()

    }

    private fun createNotificationChannels() {
        val notificationManager = NotificationManagerCompat.from(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Importance min will show normal priority notification for foreground service. See https://developer.android.com/reference/android/app/NotificationManager#IMPORTANCE_MIN
            // User has to actively configure this in the notification channel settings.
            val ongoingNotificationChannelName = if (getString(R.string.notificationChannelOngoing).trim().isNotEmpty()) getString(R.string.notificationChannelOngoing) else "Ongoing"
            NotificationChannel(NOTIFICATION_CHANNEL_ONGOING, ongoingNotificationChannelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = getString(R.string.notificationChannelOngoingDescription)
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
                setSound(null, null)
            }.run { notificationManager.createNotificationChannel(this) }


            val eventsNotificationChannelName = if (getString(R.string.events).trim().isNotEmpty()) getString(R.string.events) else "Events"
            NotificationChannel(NOTIFICATION_CHANNEL_EVENTS, eventsNotificationChannelName, NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = getString(R.string.notificationChannelEventsDescription)
                enableLights(false)
                enableVibration(false)
                setShowBadge(true)
                setSound(null, null)
            }.run { notificationManager.createNotificationChannel(this) }

            val errorNotificationChannelName = if (getString(R.string.notificationChannelErrors).trim().isNotEmpty()) getString(R.string.notificationChannelErrors) else "Errors"
            NotificationChannel(GeocoderProvider.ERROR_NOTIFICATION_CHANNEL_ID, errorNotificationChannelName, NotificationManager.IMPORTANCE_LOW).apply {
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }.run { notificationManager.createNotificationChannel(this) }

        }
    }

    @Subscribe
    fun onEvent(@Suppress("UNUSED_PARAMETER") e: RestartApp?) {
        val intent = Intent(this.applicationContext, MapActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        this.applicationContext.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        val appComponent = DaggerAppComponent.builder().app(this).build()
        appComponent.inject(this)
        return appComponent
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ONGOING = "O"
        const val NOTIFICATION_CHANNEL_EVENTS = "E"
    }
}

@Module
abstract class AppContextModule {
    @Binds
    @AppContext
    @Singleton
    abstract fun provideContext(app: App): Context
}