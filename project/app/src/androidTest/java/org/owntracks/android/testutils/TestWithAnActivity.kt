package org.owntracks.android.testutils

import android.app.Activity
import android.content.Intent
import androidx.test.espresso.intent.Intents
import com.adevinta.android.barista.rule.BaristaRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain
import org.owntracks.android.testutils.rules.ScreenshotTakingOnTestEndRule


abstract class TestWithAnActivity<T : Activity>(
    activity: Class<T>,
    private val startActivity: Boolean = true
) : TestWithCoverageEnabled() {
    private val baristaRule = BaristaRule.create(activity)
    private val screenshotRule = ScreenshotTakingOnTestEndRule()

    val activity by lazy { baristaRule.activityTestRule.activity }

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(baristaRule.activityTestRule)
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