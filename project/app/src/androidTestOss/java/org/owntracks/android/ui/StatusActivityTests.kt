package org.owntracks.android.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.rules.ScreenshotTakingOnTestEndRule
import org.owntracks.android.ui.status.StatusActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class StatusActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(StatusActivity::class.java)

    private val screenshotRule = ScreenshotTakingOnTestEndRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(baristaRule.activityTestRule)
        .around(screenshotRule)

    @Before
    fun setUp() {
        baristaRule.launchActivity()
    }

    @Test
    @AllowFlaky
    fun statusActivityShowsEndpointState() {
        assertDisplayed(R.string.status_endpoint_state_hint)
    }

    @Test
    @AllowFlaky
    fun statusActivityShowsLogsLauncher() {
        assertDisplayed(R.string.viewLogs)
    }
}