package org.owntracks.android.ui


import android.content.Intent.*
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.BundleMatchers.hasEntry
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.ui.status.logs.LogViewerActivity
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class LogViewerActivityTests :
    TestWithAnActivity<LogViewerActivity>(LogViewerActivity::class.java) {
    @Test
    fun logViewerActivityShowsTitle() {
        // Wait for the logviewer coroutine to start
        sleep(5, TimeUnit.SECONDS)
        assertDisplayed(R.string.logViewerActivityTitle)
    }

    @Test
    fun logViewerActivityExportFiresIntent() {
        clickOn(R.id.share_fab)
        intended(
            allOf(
                hasAction(ACTION_CHOOSER),
                hasExtras(
                    allOf(
                        hasEntry(
                            EXTRA_TITLE,
                            baristaRule.activityTestRule.activity.getString(R.string.exportLogFilePrompt)
                        ),
                        hasEntry(
                            `is`(EXTRA_INTENT),
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
    }
}