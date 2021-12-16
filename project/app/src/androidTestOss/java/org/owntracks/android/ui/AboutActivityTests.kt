package org.owntracks.android.ui

import android.content.Intent
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import org.hamcrest.CoreMatchers.allOf
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.scrollToPreferenceWithText
import org.owntracks.android.ui.preferences.about.AboutActivity


@LargeTest
@RunWith(AndroidJUnit4::class)
class AboutActivityTests : TestWithAnActivity<AboutActivity>(AboutActivity::class.java) {
    @Test
    @AllowFlaky
    fun documentationLinkOpensSite() {
        scrollToPreferenceWithText(R.string.preferencesDocumentation)
        clickOn(R.string.preferencesDocumentation)
        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(baristaRule.activityTestRule.activity.getString(R.string.documentationUrl))
            )
        )
    }

    @Test
    @AllowFlaky
    fun twitterLinkOpensSite() {
        scrollToPreferenceWithText(R.string.preferencesTwitter)
        clickOn(R.string.preferencesTwitter)
        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(baristaRule.activityTestRule.activity.getString(R.string.twitterUrl))
            )
        )
    }

    @Test
    @AllowFlaky
    fun sourceLinkOpensSite() {
        scrollToPreferenceWithText(R.string.preferencesRepository)
        clickOn(R.string.preferencesRepository)
        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(baristaRule.activityTestRule.activity.getString(R.string.repoUrl))
            )
        )
    }

    @Test
    @AllowFlaky
    fun translationLinkOpensSite() {
        scrollToPreferenceWithText(R.string.aboutTranslations)
        clickOn(R.string.aboutTranslations)
        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(baristaRule.activityTestRule.activity.getString(R.string.translationContributionUrl))
            )
        )
    }

    @Test
    @AllowFlaky
    fun librariesLinkListsLibraries() {
        scrollToPreferenceWithText(R.string.preferencesLicenses)
        clickOn(R.string.preferencesLicenses)
        assertDisplayed(R.string.preferencesLicenses)
    }
}