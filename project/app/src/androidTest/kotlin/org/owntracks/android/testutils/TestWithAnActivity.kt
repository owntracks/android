package org.owntracks.android.testutils

import android.app.Activity
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.cleardata.ClearDatabaseRule
import com.adevinta.android.barista.rule.cleardata.ClearFilesRule
import com.adevinta.android.barista.rule.cleardata.ClearPreferencesRule
import com.adevinta.android.barista.rule.flaky.FlakyTestRule
import dagger.hilt.android.testing.HiltAndroidRule
import javax.inject.Inject
import javax.inject.Named
import leakcanary.DetectLeaksAfterTestSuccess
import leakcanary.LeakCanary
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.owntracks.android.BaseApp
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.test.IdlingResourceWithData
import org.owntracks.android.test.SimpleIdlingResource
import org.owntracks.android.test.ThresholdIdlingResourceInterface
import org.owntracks.android.testutils.rules.ScreenshotTakingOnTestEndRule
import shark.AndroidReferenceMatchers
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
abstract class TestWithAnActivity<T : Activity>(
    activityClass: Class<T>,
    private val startActivity: Boolean = true
) : TestWithCoverageEnabled() {
  val baristaRule = BaristaRule.create(activityClass)
  private val flakyRule = FlakyTestRule().allowFlakyAttemptsByDefault(1)
  private val clearPreferencesRule: ClearPreferencesRule = ClearPreferencesRule()
  private val clearDatabaseRule: ClearDatabaseRule = ClearDatabaseRule()
  private val clearFilesRule: ClearFilesRule = ClearFilesRule()

  private val screenshotRule = ScreenshotTakingOnTestEndRule()

  @get:Rule(order = 0) @Suppress("LeakingThis") open var hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val leakRule = DetectLeaksAfterTestSuccess()

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

  fun waitForMQTTToCompleteAndContactsToBeCleared() {
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
}
