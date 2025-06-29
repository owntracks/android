package org.owntracks.android.testutils

import android.app.Activity
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.intent.Intents
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.cleardata.ClearDatabaseRule
import com.adevinta.android.barista.rule.cleardata.ClearFilesRule
import com.adevinta.android.barista.rule.cleardata.ClearPreferencesRule
import com.adevinta.android.barista.rule.flaky.FlakyTestRule
import dagger.hilt.android.testing.HiltAndroidRule
import java.lang.reflect.ParameterizedType
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.seconds
import leakcanary.DetectLeaksAfterTestSuccess
import leakcanary.LeakCanary
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain
import org.owntracks.android.BaseApp
import org.owntracks.android.R
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.test.IdlingResourceWithData
import org.owntracks.android.test.SimpleIdlingResource
import org.owntracks.android.test.ThresholdIdlingResourceInterface
import org.owntracks.android.testutils.rules.ScreenshotTakingOnTestEndRule
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.preferences.load.LoadActivity
import shark.AndroidReferenceMatchers
import timber.log.Timber

abstract class TestWithAnActivity<T : Activity>(private val startActivity: Boolean = true) :
    TestWithCoverageEnabled() {
  @Suppress("UNCHECKED_CAST")
  private val activityClass: Class<T> =
      (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<T>
  val baristaRule = BaristaRule.create(activityClass)
  private val flakyRule = FlakyTestRule().allowFlakyAttemptsByDefault(1)
  private val clearPreferencesRule: ClearPreferencesRule = ClearPreferencesRule()
  private val clearDatabaseRule: ClearDatabaseRule = ClearDatabaseRule()
  private val clearFilesRule: ClearFilesRule = ClearFilesRule()

  private val screenshotRule = ScreenshotTakingOnTestEndRule()

  @get:Rule(order = 0) @Suppress("LeakingThis") open var hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val leakRule = DetectLeaksAfterTestSuccess()

  @Inject
  @Named("mockLocationIdlingResource")
  lateinit var mockLocationIdlingResource: SimpleIdlingResource

  @Inject
  @Named("saveConfigurationIdlingResource")
  lateinit var saveConfigurationIdlingResource: SimpleIdlingResource

  @Inject lateinit var mockLocationProviderClient: LocationProviderClient

  init {
    LeakCanary.config =
        LeakCanary.config.copy(
            referenceMatchers =
                AndroidReferenceMatchers.appDefaults +
                    AndroidReferenceMatchers.instanceFieldLeak(
                        className = "android.permission.PermissionUsageHelper",
                        fieldName = "mContext",
                        description = "Android API31 leaks contexts") +
                    AndroidReferenceMatchers.instanceFieldLeak(
                        className = "android.permission.PermissionUsageHelper",
                        fieldName = "mPackageManager",
                        description = "Android API31 leaks contexts") +
                    AndroidReferenceMatchers.instanceFieldLeak(
                        className = "android.permission.PermissionUsageHelper",
                        fieldName = "mUserContexts",
                        description = "Android API31 leaks contexts") +
                    AndroidReferenceMatchers.instanceFieldLeak(
                        className = "android.app.AppOpsManager",
                        fieldName = "mContext",
                        description = "Android API31 leaks contexts") +
                    AndroidReferenceMatchers.instanceFieldLeak(
                        className = "android.app.ApplicationPackageManager",
                        fieldName = "mContext",
                        description = "Android API31 leaks contexts") +
                    AndroidReferenceMatchers.instanceFieldLeak(
                        className = "android.app.ApplicationPackageManager",
                        fieldName = "mPermissionManager",
                        description = "Android API31 leaks contexts"))
  }

  @get:Rule
  val ruleChain: RuleChain =
      RuleChain.outerRule(flakyRule)
          .around(baristaRule.activityTestRule)
          .around(clearPreferencesRule)
          .around(clearDatabaseRule)
          .around(clearFilesRule)
          .around(screenshotRule)

  @After
  fun releaseIntents() {
    Intents.release()
  }

  @Before
  fun setUp() {

    hiltRule.inject()
    Intents.init()
    if (startActivity) {
      launchActivity()
    }
  }

  fun launchActivity() {
    baristaRule.launchActivity()
  }

  fun launchActivity(intent: Intent) {
    baristaRule.launchActivity(intent)
  }

  val app: BaseApp
    get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BaseApp

  @Inject
  @Named("outgoingQueueIdlingResource")
  lateinit var outgoingQueueIdlingResource: ThresholdIdlingResourceInterface

  @Inject
  @Named("contactsClearedIdlingResource")
  lateinit var contactsClearedIdlingResource: SimpleIdlingResource

  @Inject
  @Named("mqttConnectionIdlingResource")
  lateinit var mqttConnectionIdlingResource: SimpleIdlingResource

  @Inject
  @Named("messageReceivedIdlingResource")
  lateinit var messageReceivedIdlingResource: IdlingResourceWithData<MessageBase>

  private fun waitForMQTTToCompleteAndContactsToBeCleared() {
    Timber.v("Waiting for MQTT connection to be established")
    mqttConnectionIdlingResource.use { Espresso.onIdle() }
    Timber.v("Waiting for MQTT outgoing queue to be empty")
    outgoingQueueIdlingResource.use { Espresso.onIdle() }
    Timber.v("Waiting for MQTT messages that were sent to all be received")
    messageReceivedIdlingResource.use { Espresso.onIdle() }
    contactsClearedIdlingResource.setIdleState(false)
    ContextCompat.startForegroundService(
        app,
        Intent(app, BackgroundService::class.java).apply {
          action = "org.owntracks.android.CLEAR_CONTACTS"
        })
    Timber.v("Waiting for contacts to be cleared")
    contactsClearedIdlingResource.use { Espresso.onIdle() }
    Timber.v("Test setup complete")
  }

  internal fun setupTestActivity(configureConnection: () -> Unit) {
    PreferenceManager.getDefaultSharedPreferences(app)
        .edit()
        .putInt(Preferences::monitoring.name, MonitoringMode.Quiet.value)
        .putString(Preferences::reverseGeocodeProvider.name, "None")
        .apply()
    setNotFirstStartPreferences()
    launchActivity()

    if (activityClass == MapActivity::class.java) {
      grantMapActivityPermissions()
    }

    configureConnection()

    waitUntilActivityVisible()
    waitForMQTTToCompleteAndContactsToBeCleared()
  }

  fun waitUntilActivityVisible() {
    waitUntilActivityVisible(activityClass)
  }

  fun waitUntilActivityVisible(clazz: Class<out Activity>) {
    val startTime = System.currentTimeMillis()
    Timber.d("Waiting for ${activityClass.simpleName} to be visible")
    while (!clazz.isInstance(getCurrentActivity())) {
      Thread.sleep(CONDITION_CHECK_INTERVAL)
      if (System.currentTimeMillis() - startTime >= TIMEOUT) {
        throw AssertionError(
            "Activity ${activityClass.simpleName} not visible after $TIMEOUT milliseconds",
        )
      }
    }
    Timber.d("${activityClass.simpleName} is now visible")
  }

  fun reportLocationFromMap(
      locationIdlingResource: IdlingResource?,
      mockLocationFunction: () -> Unit = {}
  ) {
    if (getCurrentActivity() !is MapActivity && getCurrentActivity() !is LoadActivity) {
      openDrawer()
      clickOn(R.string.title_activity_map)
    }
    waitUntilActivityVisible()
    clickOn(R.id.menu_monitoring)
    clickOn(R.id.fabMonitoringModeMove)
    mockLocationFunction()

    locationIdlingResource.use(5.seconds) {
      clickOn(R.id.fabMyLocation)
      clickOn(R.id.menu_report)
    }
  }
}
