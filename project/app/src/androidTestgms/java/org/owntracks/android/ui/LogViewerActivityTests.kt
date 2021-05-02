package org.owntracks.android.ui


import android.content.Intent.*
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.BundleMatchers.hasEntry
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions.sleep
import com.schibsted.spain.barista.rule.BaristaRule
import com.schibsted.spain.barista.rule.flaky.AllowFlaky
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.ScreenshotTakingOnTestEndRule
import org.owntracks.android.ui.status.logs.LogViewerActivity
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class LogViewerActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(LogViewerActivity::class.java)

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
    @AllowFlaky(attempts = 1)
    fun logViewerActivityShowsTitle() {
        // Wait for the logviewer coroutine to start
        sleep(5, TimeUnit.SECONDS)
        assertDisplayed(R.string.logViewerActivityTitle)
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun logViewerActivityExportFiresIntent() {
        try {
            Intents.init()
            sleep(1, TimeUnit.SECONDS)
            clickOn(R.id.share_fab)
            intended(
                    allOf(
                            hasAction(ACTION_CHOOSER),
                            hasExtras(
                                    allOf(
                                            hasEntry(EXTRA_TITLE, baristaRule.activityTestRule.activity.getString(R.string.exportLogFilePrompt)),
                                            hasEntry(`is`(EXTRA_INTENT),
                                                    allOf(
                                                            hasAction(ACTION_SEND),
                                                            hasFlag(FLAG_GRANT_READ_URI_PERMISSION),
                                                            hasType("text/plain")
                                                    )
                                            )
                                    )
                            )
                    )
            )
        } finally {
            Intents.release()
        }
    }
}