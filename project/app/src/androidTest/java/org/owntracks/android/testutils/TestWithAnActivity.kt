package org.owntracks.android.testutils

import android.app.Activity
import androidx.test.espresso.intent.Intents
import com.adevinta.android.barista.rule.BaristaRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain
import org.owntracks.android.testutils.rules.ScreenshotTakingOnTestEndRule
import org.owntracks.android.testutils.rules.WaypointsObjectBoxClearRule

abstract class TestWithAnActivity<T : Activity>(
    activity: Class<T>,
    private val startActivity: Boolean = true
) : TestWithCoverageEnabled() {
    @get:Rule
    var baristaRule = BaristaRule.create(activity)

    private val screenshotRule = ScreenshotTakingOnTestEndRule()

    private val objectStoreClearRule = WaypointsObjectBoxClearRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(baristaRule.activityTestRule)
        .around(screenshotRule).around(objectStoreClearRule)


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
            baristaRule.launchActivity()
        }
    }
}

