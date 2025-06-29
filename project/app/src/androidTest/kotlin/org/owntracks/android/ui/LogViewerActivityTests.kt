package org.owntracks.android.ui

import android.content.Intent.ACTION_CHOOSER
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_INTENT
import android.content.Intent.EXTRA_TITLE
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.BundleMatchers.hasEntry
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtras
import androidx.test.espresso.intent.matcher.IntentMatchers.hasFlag
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.filters.MediumTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.concurrent.TimeUnit
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.junit.Rule
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.ui.status.logs.LogViewerActivity

@MediumTest
@HiltAndroidTest
class LogViewerActivityTests : TestWithAnActivity<LogViewerActivity>() {
  @get:Rule(order = 0) override var hiltRule = HiltAndroidRule(this)

  @Test
  fun log_viewer_activity_shows_title() {
    // Wait for the logviewer coroutine to start
    sleep(5, TimeUnit.SECONDS)
    assertDisplayed(R.string.logViewerActivityTitle)
  }

  @Test
  fun log_viewer_activity_export_fires_intent() {
    clickOn(R.id.share_fab)
    intended(
        allOf(
            hasAction(ACTION_CHOOSER),
            hasExtras(
                allOf(
                    hasEntry(
                        EXTRA_TITLE,
                        baristaRule.activityTestRule.activity.getString(
                            R.string.exportLogFilePrompt)),
                    hasEntry(
                        `is`(EXTRA_INTENT),
                        allOf(
                            hasAction(ACTION_SEND),
                            hasFlag(FLAG_GRANT_READ_URI_PERMISSION),
                            hasType("text/plain")))))))
  }
}
