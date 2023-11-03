package org.owntracks.android.testutils

import android.app.Activity
import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.cleardata.ClearDatabaseRule
import com.adevinta.android.barista.rule.cleardata.ClearFilesRule
import com.adevinta.android.barista.rule.cleardata.ClearPreferencesRule
import com.adevinta.android.barista.rule.flaky.FlakyTestRule
import leakcanary.DetectLeaksAfterTestSuccess
import leakcanary.LeakCanary
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain
import org.owntracks.android.App
import org.owntracks.android.testutils.rules.ScreenshotTakingOnTestEndRule
import shark.AndroidReferenceMatchers

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

    @get:Rule
    val leakRule = DetectLeaksAfterTestSuccess()

    init {
        LeakCanary.config = LeakCanary.config.copy(
            referenceMatchers = AndroidReferenceMatchers.appDefaults +
                AndroidReferenceMatchers.instanceFieldLeak(
                    className = "android.permission.PermissionUsageHelper",
                    fieldName = "mContext",
                    description = "Android API31 leaks contexts"
                ) +
                AndroidReferenceMatchers.instanceFieldLeak(
                    className = "android.permission.PermissionUsageHelper",
                    fieldName = "mPackageManager",
                    description = "Android API31 leaks contexts"
                ) +
                AndroidReferenceMatchers.instanceFieldLeak(
                    className = "android.permission.PermissionUsageHelper",
                    fieldName = "mUserContexts",
                    description = "Android API31 leaks contexts"
                ) +
                AndroidReferenceMatchers.instanceFieldLeak(
                    className = "android.app.AppOpsManager",
                    fieldName = "mContext",
                    description = "Android API31 leaks contexts"
                ) +
                AndroidReferenceMatchers.instanceFieldLeak(
                    className = "android.app.ApplicationPackageManager",
                    fieldName = "mContext",
                    description = "Android API31 leaks contexts"
                ) +
                AndroidReferenceMatchers.instanceFieldLeak(
                    className = "android.app.ApplicationPackageManager",
                    fieldName = "mPermissionManager",
                    description = "Android API31 leaks contexts"
                )
        )
    }

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(flakyRule)
        .around(baristaRule.activityTestRule)
        .around(clearPreferencesRule)
        .around(clearDatabaseRule)
        .around(clearFilesRule)
        .around(screenshotRule)

    @Before
    fun initIntents() {
        Intents.init()
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

    @Before
    fun setUp() {
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

    val app: App
        get() = InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .applicationContext as App
}
