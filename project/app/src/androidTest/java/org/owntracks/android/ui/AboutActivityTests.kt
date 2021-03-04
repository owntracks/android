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
import com.schibsted.spain.barista.interaction.BaristaScrollInteractions.scrollTo
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
    @AllowFlaky(attempts = 1)
    fun documentationLinkOpensSite() {
        clickOn(R.string.preferencesDocumentation)
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(baristaRule.activityTestRule.activity.getString(R.string.documentationUrl))))
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun twitterLinkOpensSite() {
        scrollTo(R.string.preferencesTwitter)
        clickOn(R.string.preferencesTwitter)
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(baristaRule.activityTestRule.activity.getString(R.string.twitterUrl))))
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun sourceLinkOpensSite() {
        clickOn(R.string.preferencesRepository)
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(baristaRule.activityTestRule.activity.getString(R.string.repoUrl))))
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun translationLinkOpensSite() {
        clickOn(R.string.aboutTranslations)
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(baristaRule.activityTestRule.activity.getString(R.string.translationContributionUrl))))
    }
    
    @Test
    @AllowFlaky(attempts = 1)
    fun librariesLinkListsLibraries() {
        clickOn(R.string.preferencesLicenses)
        assertDisplayed(R.string.preferencesLicenses)
    }
}