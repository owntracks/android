package org.owntracks.android.ui

import android.app.Activity.RESULT_OK
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import org.hamcrest.CoreMatchers.allOf
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.scrollToPreferenceWithText
import org.owntracks.android.ui.preferences.about.AboutActivity

@SmallTest
@RunWith(AndroidJUnit4::class)
class AboutActivityTests : TestWithAnActivity<AboutActivity>(AboutActivity::class.java) {
    @Test
    fun documentationLinkOpensSite() {
        scrollToPreferenceWithText(R.string.preferencesDocumentation)
        val matcher = allOf(
            hasAction(Intent.ACTION_VIEW),
            hasData(baristaRule.activityTestRule.activity.getString(R.string.documentationUrl))
        )
        intending(matcher).respondWith(Instrumentation.ActivityResult(RESULT_OK,null))
        clickOn(R.string.preferencesDocumentation)
        intended(matcher)
    }

    @Test
    fun sourceLinkOpensSite() {
        scrollToPreferenceWithText(R.string.preferencesRepository)
        val matcher = allOf(
            hasAction(Intent.ACTION_VIEW),
            hasData(baristaRule.activityTestRule.activity.getString(R.string.repoUrl))
        )
        intending(matcher).respondWith(Instrumentation.ActivityResult(RESULT_OK,null))
        clickOn(R.string.preferencesRepository)
        intended(
            matcher
        )
    }

    @Test
    fun translationLinkOpensSite() {
        scrollToPreferenceWithText(R.string.aboutTranslations)
        val matcher = allOf(
            hasAction(Intent.ACTION_VIEW),
            hasData(baristaRule.activityTestRule.activity.getString(R.string.translationContributionUrl))
        )
        intending(matcher).respondWith(Instrumentation.ActivityResult(RESULT_OK,null))
        clickOn(R.string.aboutTranslations)
        intended(matcher)
    }

    @Test
    fun librariesLinkListsLibraries() {
        scrollToPreferenceWithText(R.string.preferencesLicenses)
        clickOn(R.string.preferencesLicenses)
        assertDisplayed(R.string.preferencesLicenses)
    }
}
