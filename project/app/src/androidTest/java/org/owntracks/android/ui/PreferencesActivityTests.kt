package org.owntracks.android.ui

import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import com.schibsted.spain.barista.rule.BaristaRule
import com.schibsted.spain.barista.rule.flaky.AllowFlaky
import org.hamcrest.CoreMatchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.ui.preferences.PreferencesActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class PreferencesActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(PreferencesActivity::class.java)


    @Before
    fun setUp() {
        baristaRule.launchActivity()
    }


    @Test
    @AllowFlaky(attempts = 1)
    fun initialViewShowsTopLevelMenu() {
        assertDisplayed(R.string.preferencesServer)
        assertDisplayed(R.string.preferencesReporting)
        assertDisplayed(R.string.preferencesNotification)
        assertDisplayed(R.string.preferencesAdvanced)
        assertDisplayed(R.string.configurationManagement)
        assertDisplayed(R.string.preferencesInfo)
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun documentationLinkOpensSite() {
        Intents.init()
        clickOn(R.string.preferencesInfo)
        clickOn(R.string.preferencesDocumentation)
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(baristaRule.activityTestRule.activity.getString(R.string.preferencesDocumentationSummary))))
        Intents.release()
    }
}