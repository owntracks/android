package org.owntracks.android

import android.app.ActivityManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.os.StrictMode
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.MutableLiveData
import androidx.work.Configuration
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.EarlyEntryPoint
import dagger.hilt.android.EarlyEntryPoints
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import java.security.Security
import javax.inject.Provider
import kotlinx.datetime.Instant
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.conscrypt.Conscrypt
import org.owntracks.android.data.waypoints.RoomWaypointsRepo
import org.owntracks.android.di.CustomBindingComponentBuilder
import org.owntracks.android.di.CustomBindingEntryPoint
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.logging.TimberInMemoryLogTree
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.PreferencesStore
import org.owntracks.android.preferences.types.AppTheme
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.RunThingsOnOtherThreads
import org.owntracks.android.support.receiver.StartBackgroundServiceReceiver
import timber.log.Timber

@HiltAndroidApp
class App : BaseApp() {
  override fun onCreate() {
    super.onCreate()
    StartBackgroundServiceReceiver.enable(this)
  }
}

open class BaseApp :
    Application(),
    Configuration.Provider,
    Preferences.OnPreferenceChangeListener,
    ComponentCallbacks2 {

  @EarlyEntryPoint
  @InstallIn(SingletonComponent::class)
  internal interface ApplicationEntrypoint {
    fun preferences(): Preferences

    fun workerFactory(): HiltWorkerFactory

    fun scheduler(): Scheduler

    fun bindingComponentProvider(): Provider<CustomBindingComponentBuilder>

    fun messageProcessor(): MessageProcessor

    fun notificationManager(): NotificationManagerCompat

    fun preferencesStore(): PreferencesStore

    fun runThingsOnOtherThreads(): RunThingsOnOtherThreads

    fun roomWaypointsRepo(): RoomWaypointsRepo
  }

  private val preferences by lazy {
    EarlyEntryPoints.get(this, ApplicationEntrypoint::class.java).preferences()
  }

  private val workerFactory: HiltWorkerFactory by lazy {
    EarlyEntryPoints.get(this, ApplicationEntrypoint::class.java).workerFactory()
  }

  private val scheduler: Scheduler by lazy {
    EarlyEntryPoints.get(this, ApplicationEntrypoint::class.java).scheduler()
  }

  private val bindingComponentProvider: Provider<CustomBindingComponentBuilder> by lazy {
    EarlyEntryPoints.get(this, ApplicationEntrypoint::class.java).bindingComponentProvider()
  }

  private val notificationManager: NotificationManagerCompat by lazy {
    EarlyEntryPoints.get(this, ApplicationEntrypoint::class.java).notificationManager()
  }

  private val preferencesStore: PreferencesStore by lazy {
    EarlyEntryPoints.get(this, ApplicationEntrypoint::class.java).preferencesStore()
  }

  private val runThingsOnOtherThreads: RunThingsOnOtherThreads by lazy {
    EarlyEntryPoints.get(this, ApplicationEntrypoint::class.java).runThingsOnOtherThreads()
  }

  val workManagerFailedToInitialize = MutableLiveData(false)

  override fun onCreate() {
    // Make sure we use Conscrypt for advanced TLS features on all devices.
    Security.insertProviderAt(Conscrypt.newProviderBuilder().provideTrustManager(true).build(), 1)

    // Bring in a real version of BC and don't use the device version.
    Security.removeProvider("BC")
    Security.addProvider(BouncyCastleProvider())

    super.onCreate()

    setGlobalExceptionHandler()

    val dataBindingComponent = bindingComponentProvider.get().build()
    val dataBindingEntryPoint =
        EntryPoints.get(dataBindingComponent, CustomBindingEntryPoint::class.java)

    DataBindingUtil.setDefaultComponent(dataBindingEntryPoint)

    scheduler.cancelAllTasks()
    Timber.plant(TimberInMemoryLogTree(BuildConfig.DEBUG))

    if (BuildConfig.DEBUG) {
      System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "debug")
      org.slf4j.LoggerFactory.getLogger(this::class.java).trace("SLF4J logging at trace level")
      Timber.e("StrictMode enabled in DEBUG build")
      StrictMode.setThreadPolicy(
          StrictMode.ThreadPolicy.Builder()
              .detectNetwork()
              .penaltyFlashScreen()
              .penaltyDialog()
              .build())
      StrictMode.setVmPolicy(
          StrictMode.VmPolicy.Builder()
              .detectLeakedSqlLiteObjects()
              .detectLeakedClosableObjects()
              .detectFileUriExposure()
              .penaltyLog()
              .build())
    }

    preferences.registerOnPreferenceChangedListener(this)

    setThemeFromPreferences()

    // Notifications can be sent from multiple places, so let's make sure we've got the channels in
    // place
    createNotificationChannels()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      (this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
          .getHistoricalProcessExitReasons(this.packageName, 0, 10)
          .firstOrNull()
          ?.run {
            Timber.i(
                "Historical process exited at ${Instant.fromEpochMilliseconds(timestamp)}. reason: $description, status: $status, reason: $reason")
          }
    }
    applicationContext.noBackupFilesDir.resolve("crash.log").run {
      if (exists()) {
        readText().let { Timber.e("Previous crash: $it") }
        delete()
      }
    }
  }

  private fun setGlobalExceptionHandler() {
    val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
      try {
        applicationContext.noBackupFilesDir
            .resolve("crash.log")
            .writeText(
                """
          |Thread: ${t.name}
          |Exception: ${e.message}
          |Stacktrace:
          |${e.stackTrace.joinToString("\n\t")}
          """
                    .trimMargin())
      } catch (e: Exception) {
        Timber.e(e, "Error writing crash log")
      }
      currentHandler?.uncaughtException(t, e)
    }
  }

  @MainThread
  private fun setThemeFromPreferences() {
    when (preferences.theme) {
      AppTheme.Auto -> AppCompatDelegate.setDefaultNightMode(Preferences.SYSTEM_NIGHT_AUTO_MODE)
      AppTheme.Dark -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
      AppTheme.Light -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      // Importance min will show normal priority notification for foreground service. See
      // https://developer.android.com/reference/android/app/NotificationManager#IMPORTANCE_MIN
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
              NotificationManager.IMPORTANCE_LOW)
          .apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            description = getString(R.string.notificationChannelOngoingDescription)
            enableLights(false)
            enableVibration(false)
            setShowBadge(false)
            setSound(null, null)
          }
          .run { notificationManager.createNotificationChannel(this) }

      val eventsNotificationChannelName =
          if (getString(R.string.events).trim().isNotEmpty()) {
            getString(R.string.events)
          } else {
            "Events"
          }
      NotificationChannel(
              NOTIFICATION_CHANNEL_EVENTS,
              eventsNotificationChannelName,
              NotificationManager.IMPORTANCE_HIGH)
          .apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            description = getString(R.string.notificationChannelEventsDescription)
            enableLights(false)
            enableVibration(false)
            setShowBadge(true)
            setSound(null, null)
          }
          .run { notificationManager.createNotificationChannel(this) }

      val errorNotificationChannelName =
          if (getString(R.string.notificationChannelErrors).trim().isNotEmpty()) {
            getString(R.string.notificationChannelErrors)
          } else {
            "Errors"
          }
      NotificationChannel(
              GeocoderProvider.ERROR_NOTIFICATION_CHANNEL_ID,
              errorNotificationChannelName,
              NotificationManager.IMPORTANCE_LOW)
          .apply { lockscreenVisibility = Notification.VISIBILITY_PRIVATE }
          .run { notificationManager.createNotificationChannel(this) }
    }
  }

  override fun onPreferenceChanged(properties: Set<String>) {
    if (properties.contains(Preferences::theme.name)) {
      Timber.d("Theme changed. Setting theme to ${preferences.theme}")
      // Can only call setThemeFromPreferences on the main thread
      runThingsOnOtherThreads.postOnMainHandlerDelayed(::setThemeFromPreferences, 0)
    }
    Timber.v("Idling preferenceSetIdlingResource because of $properties")
  }

  /** Migrate preferences. Available to be called from espresso tests. */
  @VisibleForTesting
  fun migratePreferences() {
    preferencesStore.migrate()
  }

  override fun onTrimMemory(level: Int) {
    Timber.w(
        "onTrimMemory notified ${getAvailableMemory().run { "isLowMemory: $lowMemory availMem: ${android.text.format.Formatter.formatShortFileSize(applicationContext,availMem)}, threshold: ${android.text.format.Formatter.formatShortFileSize(applicationContext,threshold)} totalMemory: ${android.text.format.Formatter.formatShortFileSize(applicationContext,totalMem)} " }}")
    super.onTrimMemory(level)
  }

  override fun onLowMemory() {
    Timber.w(
        "onLowMemory notified ${getAvailableMemory().run { "isLowMemory: $lowMemory availMem: ${android.text.format.Formatter.formatShortFileSize(applicationContext,availMem)}, threshold: ${android.text.format.Formatter.formatShortFileSize(applicationContext,threshold)} totalMemory: ${android.text.format.Formatter.formatShortFileSize(applicationContext,totalMem)} " }}")
    super.onLowMemory()
  }

  private fun getAvailableMemory(): ActivityManager.MemoryInfo {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return ActivityManager.MemoryInfo().also { memoryInfo ->
      activityManager.getMemoryInfo(memoryInfo)
    }
  }

  companion object {
    const val NOTIFICATION_CHANNEL_ONGOING = "O"
    const val NOTIFICATION_CHANNEL_EVENTS = "E"
    const val NOTIFICATION_ID_ONGOING = 1
    const val NOTIFICATION_ID_EVENT_GROUP = 2
    const val NOTIFICATION_GROUP_EVENTS = "events"
  }

  override val workManagerConfiguration: Configuration
    get() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setInitializationExceptionHandler { throwable ->
              Timber.e(throwable, "Exception thrown when initializing WorkManager")
              workManagerFailedToInitialize.postValue(true)
            }
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
