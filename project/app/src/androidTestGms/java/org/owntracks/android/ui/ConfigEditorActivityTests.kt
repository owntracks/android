package org.owntracks.android.ui

import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.ScreenshotTakingOnTestEndRule
import org.owntracks.android.ui.preferences.editor.EditorActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class ConfigEditorActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(EditorActivity::class.java)

    private val screenshotRule = ScreenshotTakingOnTestEndRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(baristaRule.activityTestRule)
        .around(screenshotRule)

    @Before
    fun initIntents() {
        Intents.init()
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

    @Before
    fun setUp() {
        baristaRule.launchActivity()
    }

    @Test
    @AllowFlaky
    fun configurationManagementCanEditASetType() {
        openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
        clickOn(R.string.preferencesEditor)
        writeTo(
            R.id.inputKey,
            baristaRule.activityTestRule.activity.getString(R.string.preferenceKeyExperimentalFeatures)
        )
        writeTo(R.id.inputValue, "this, that,    other")
        clickDialogPositiveButton()
        assertContains(
            R.id.effectiveConfiguration,
            "\"experimentalFeatures\" : [ \"other\", \"that\", \"this\" ]"
        )
    }

    @Test
    @AllowFlaky
    fun configurationManagementCanEditAStringType() {
        openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
        clickOn(R.string.preferencesEditor)
        writeTo(
            R.id.inputKey,
            baristaRule.activityTestRule.activity.getString(R.string.preferenceKeyModeId)
        )
        writeTo(R.id.inputValue, "0")
        clickDialogPositiveButton()

        openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
        clickOn(R.string.preferencesEditor)
        writeTo(
            R.id.inputKey,
            baristaRule.activityTestRule.activity.getString(R.string.preferenceKeyHost)
        )
        writeTo(R.id.inputValue, "example.com")
        clickDialogPositiveButton()

        assertContains(
            R.id.effectiveConfiguration,
            "\"host\" : \"example.com\""
        )
    }

    @Test
    @AllowFlaky
    fun configurationManagementCanEditABooleanType() {
        openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
        clickOn(R.string.preferencesEditor)
        writeTo(
            R.id.inputKey,
            baristaRule.activityTestRule.activity.getString(R.string.preferenceKeyRemoteCommand)
        )
        writeTo(R.id.inputValue, "false")
        clickDialogPositiveButton()
        assertContains(R.id.effectiveConfiguration, "\"cmd\" : false")
    }

    @Test
    @AllowFlaky
    fun editorActivityShowsDefaultConfig() {
        assertContains(
            R.id.effectiveConfiguration,
            "\"_type\" : \"configuration\""
        )
    }

    @Test
    @AllowFlaky
    fun editorCanExportConfig() {
        val chooserIntentMatcher = allOf(
            hasAction(Intent.ACTION_CHOOSER),
            hasExtra(
                `is`(Intent.EXTRA_INTENT),
                allOf(
                    hasAction(Intent.ACTION_SEND),
                    hasType("text/plain")
                )
            ),
            hasExtra(Intent.EXTRA_TITLE, "Export")
        )
        intending(anyIntent()).respondWithFunction { Instrumentation.ActivityResult(0, null) }

        openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
        clickOn(R.string.exportConfiguration)

        intended(chooserIntentMatcher)
    }
}