package org.owntracks.android

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import dagger.hilt.EntryPoints
import dagger.hilt.android.HiltAndroidApp
import org.conscrypt.Conscrypt
import org.owntracks.android.di.CustomBindingComponentBuilder
import org.owntracks.android.di.CustomBindingEntryPoint
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.logging.TimberInMemoryLogTree
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.Preferences
import org.owntracks.android.support.RunThingsOnOtherThreads
import timber.log.Timber
import java.security.Security
import javax.inject.Inject
import javax.inject.Provider
import kotlin.system.measureNanoTime

@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var runThingsOnOtherThreads: RunThingsOnOtherThreads

    @Inject
    lateinit var messageProcessor: MessageProcessor

    @Inject
    lateinit var workerFactory: WorkerFactory

    @Inject
    lateinit var scheduler: Scheduler

    @Inject
    lateinit var bindingComponentProvider: Provider<CustomBindingComponentBuilder>

    override fun onCreate() {
        // Make sure we use Conscrypt for advanced TLS features on all devices.
        // X509ExtendedTrustManager not available pre-24, fall back to device. https://github.com/google/conscrypt/issues/603
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Security.insertProviderAt(
                Conscrypt.newProviderBuilder().provideTrustManager(true).build(), 1
            )
        } else {
            Security.insertProviderAt(
                Conscrypt.newProviderBuilder().provideTrustManager(false).build(), 1
            )
        }

        super.onCreate()

        val dataBindingComponent = bindingComponentProvider.get().build()
        val dataBindingEntryPoint = EntryPoints.get(
            dataBindingComponent, CustomBindingEntryPoint::class.java
        )

        DataBindingUtil.setDefaultComponent(dataBindingEntryPoint)

        WorkManager.initialize(
            this,
            Configuration.Builder().setWorkerFactory(workerFactory).build()
        )
        scheduler.cancelAllTasks()
        Timber.plant(TimberInMemoryLogTree(BuildConfig.DEBUG))
        if (BuildConfig.DEBUG) {
            Timber.e("StrictMode enabled in DEBUG build")
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectNetwork()
                    .penaltyFlashScreen()
                    .penaltyDialog()
                    .build()
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
        preferences.checkFirstStart()

        // Running this on a background thread will deadlock FirebaseJobDispatcher.
        // Initialize will call Scheduler to connect off the main thread anyway.
        runThingsOnOtherThreads.postOnMainHandlerDelayed({ messageProcessor.initialize() }, 510)


        when (preferences.theme) {
            Preferences.NIGHT_MODE_AUTO -> AppCompatDelegate.setDefaultNightMode(Preferences.SYSTEM_NIGHT_AUTO_MODE)
            Preferences.NIGHT_MODE_ENABLE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Preferences.NIGHT_MODE_DISABLE -> AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Notifications can be sent from multiple places, so let's make sure we've got the channels in place
        createNotificationChannels()

    }

    private fun createNotificationChannels() {
        val notificationManager = NotificationManagerCompat.from(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Importance min will show normal priority notification for foreground service. See https://developer.android.com/reference/android/app/NotificationManager#IMPORTANCE_MIN
            // User has to actively configure this in the notification channel settings.
            val ongoingNotificationChannelName =
                if (getString(R.string.notificationChannelOngoing).trim()
                        .isNotEmpty()
                ) getString(R.string.notificationChannelOngoing) else "Ongoing"
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


            val eventsNotificationChannelName = if (getString(R.string.events).trim()
                    .isNotEmpty()
            ) getString(R.string.events) else "Events"
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

            val errorNotificationChannelName =
                if (getString(R.string.notificationChannelErrors).trim()
                        .isNotEmpty()
                ) getString(R.string.notificationChannelErrors) else "Errors"
            NotificationChannel(
                GeocoderProvider.ERROR_NOTIFICATION_CHANNEL_ID,
                errorNotificationChannelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }.run { notificationManager.createNotificationChannel(this) }

        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ONGOING = "O"
        const val NOTIFICATION_CHANNEL_EVENTS = "E"
    }
}

inline fun perfLog(description: String, block: () -> Unit) {
    if (BuildConfig.DEBUG) {
        val elapsed = measureNanoTime { block() }
        Timber.tag("PERF").e("$description: ${elapsed / 1_000_000}ms")
    }
}

inline fun perfLog(block: () -> Unit) {
    if (BuildConfig.DEBUG) {
        val caller =
            Thread.currentThread().stackTrace[2].let { "${it.className}/ ${it.methodName}" }
        perfLog(caller, block)
    }
}