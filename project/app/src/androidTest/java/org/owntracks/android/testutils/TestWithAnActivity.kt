package org.owntracks.android.testutils

import android.app.Activity
import android.content.Intent
import androidx.test.espresso.intent.Intents
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.cleardata.ClearDatabaseRule
import com.adevinta.android.barista.rule.cleardata.ClearFilesRule
import com.adevinta.android.barista.rule.cleardata.ClearPreferencesRule
import com.adevinta.android.barista.rule.flaky.FlakyTestRule
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain
import org.owntracks.android.testutils.rules.ScreenshotTakingOnTestEndRule


abstract class TestWithAnActivity<T : Activity>(
    activityClass: Class<T>,
    private val startActivity: Boolean = true
) : TestWithCoverageEnabled() {
    val baristaRule = BaristaRule.create(activityClass)
    private val flakyRule = FlakyTestRule().allowFlakyAttemptsByDefault(3)
    private val clearPreferencesRule: ClearPreferencesRule = ClearPreferencesRule()
    private val clearDatabaseRule: ClearDatabaseRule = ClearDatabaseRule()
    private val clearFilesRule: ClearFilesRule = ClearFilesRule()

    private val screenshotRule = ScreenshotTakingOnTestEndRule()

    @get:Rule
    val leakRule = DetectLeaksAfterTestSuccess()

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
}