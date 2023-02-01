package org.owntracks.android

import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.StrictMode
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.IdlingResource
import androidx.work.Configuration
import androidx.work.WorkerFactory
import dagger.hilt.EntryPoints
import dagger.hilt.android.HiltAndroidApp
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.conscrypt.Conscrypt
import org.owntracks.android.di.CustomBindingComponentBuilder
import org.owntracks.android.di.CustomBindingEntryPoint
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.logging.TimberInMemoryLogTree
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.AppTheme
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.RunThingsOnOtherThreads
import org.owntracks.android.ui.AppShortcuts
import timber.log.Timber
import java.security.Security
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class App : Application(), Configuration.Provider, Preferences.OnPreferenceChangeListener {
    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var runThingsOnOtherThreads: RunThingsOnOtherThreads

    @Inject
    lateinit var workerFactory: WorkerFactory

    @Inject
    lateinit var scheduler: Scheduler

    @Inject
    lateinit var bindingComponentProvider: Provider<CustomBindingComponentBuilder>

    @Inject
    lateinit var appShortcuts: AppShortcuts

    @Inject
    lateinit var messageProcessor: MessageProcessor

    val workManagerFailedToInitialize = MutableLiveData(false)

    override fun onCreate() {
        // Make sure we use Conscrypt for advanced TLS features on all devices.
        Security.insertProviderAt(
            Conscrypt.newProviderBuilder()
                .provideTrustManager(true)
                .build(),
            1
        )

        // Bring in a real version of BC and don't use the device version.
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())

        super.onCreate()

        val dataBindingComponent = bindingComponentProvider.get()
            .build()
        val dataBindingEntryPoint = EntryPoints.get(
            dataBindingComponent,
            CustomBindingEntryPoint::class.java
        )

        DataBindingUtil.setDefaultComponent(dataBindingEntryPoint)

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

        preferences.registerOnPreferenceChangedListener(this)

        setThemeFromPreferences()
        appShortcuts.enableShortcuts(this)

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
            }
                .run { notificationManager.createNotificationChannel(this) }

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
            }
                .run { notificationManager.createNotificationChannel(this) }

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
            }
                .run { notificationManager.createNotificationChannel(this) }
        }
    }

    @get:VisibleForTesting
    val mqttConnectionIdlingResource: IdlingResource?
        get() = messageProcessor.mqttConnectionIdlingResource


    companion object {
        const val NOTIFICATION_CHANNEL_ONGOING = "O"
        const val NOTIFICATION_CHANNEL_EVENTS = "E"
    }

    @SuppressLint("RestrictedApi")
    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setInitializationExceptionHandler { throwable ->
                Timber.e(throwable, "Exception thrown when initializing WorkManager")
                workManagerFailedToInitialize.postValue(true)
            }
            .build()

    override fun onPreferenceChanged(properties: List<String>) {
        if (properties.contains(Preferences::theme.name)) {
            Timber.d("Theme changed. Setting theme to ${preferences.theme}")
            setThemeFromPreferences()
        }
    }
}
