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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.ScreenshotTakingOnTestEndRule
import org.owntracks.android.e2e.scrollToPreferenceWithText
import org.owntracks.android.ui.preferences.about.AboutActivity


@LargeTest
@RunWith(AndroidJUnit4::class)
class AboutActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(AboutActivity::class.java)

    private val screenshotRule = ScreenshotTakingOnTestEndRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(baristaRule.activityTestRule)
        .around(screenshotRule)

    @Before
    fun setUp() {
        baristaRule.launchActivity()
    }

    @Before
    fun initIntents() {
        Intents.init()
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

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