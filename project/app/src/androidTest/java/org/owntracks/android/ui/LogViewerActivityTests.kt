package org.owntracks.android.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions.sleep
import com.schibsted.spain.barista.rule.BaristaRule
import com.schibsted.spain.barista.rule.flaky.AllowFlaky
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.ScreenshotTakingOnFailureRule
import org.owntracks.android.ui.preferences.logs.LogViewerActivity
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class LogViewerActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(LogViewerActivity::class.java)

    private val screenshotRule = ScreenshotTakingOnFailureRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
            .outerRule(baristaRule.activityTestRule)
            .around(screenshotRule)

    @Before
    fun setUp() {
        baristaRule.launchActivity()
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun statusActivityShowsEndpointState() {
        // Wait for the logviewer coroutine to start
        sleep(5, TimeUnit.SECONDS)
        BaristaVisibilityAssertions.assertDisplayed(R.string.logViewerActivityTitle)
    }
}