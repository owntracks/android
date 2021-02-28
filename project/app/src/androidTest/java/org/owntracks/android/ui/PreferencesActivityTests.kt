package org.owntracks.android.ui

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickBack
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import com.schibsted.spain.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.schibsted.spain.barista.interaction.BaristaEditTextInteractions.writeTo
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
import org.owntracks.android.ui.preferences.PreferencesActivity
import org.owntracks.android.ui.preferences.clickOnAndWait


@LargeTest
@RunWith(AndroidJUnit4::class)
class PreferencesActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(PreferencesActivity::class.java)

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
    fun initialViewShowsTopLevelMenu() {
        assertDisplayed(R.string.preferencesServer)
        assertDisplayed(R.string.preferencesReporting)
        assertDisplayed(R.string.preferencesNotification)
        assertDisplayed(R.string.preferencesAdvanced)
        assertDisplayed(R.string.configurationManagement)
        assertDisplayed(R.string.title_activity_about)
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun documentationLinkOpensSite() {
        clickOn(R.string.title_activity_about)
        clickOn(R.string.preferencesDocumentation)
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(baristaRule.activityTestRule.activity.getString(R.string.documentationUrl))))
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun twitterLinkOpensSite() {
        clickOn(R.string.title_activity_about)
        clickOn(R.string.preferencesTwitter)
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(baristaRule.activityTestRule.activity.getString(R.string.twitterUrl))))
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun sourceLinkOpensSite() {
        clickOn(R.string.title_activity_about)
        clickOn(R.string.preferencesRepository)
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(baristaRule.activityTestRule.activity.getString(R.string.repoUrl))))
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun librariesLinkListsLibraries() {
        clickOn(R.string.title_activity_about)
        clickOn(R.string.preferencesLicenses)
        assertDisplayed(R.string.preferencesLicenses)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun configurationManagementCanEditASetType() {
        clickOn(R.string.configurationManagement)
        openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
        clickOn(R.string.preferencesEditor)
        writeTo(R.id.inputKey, baristaRule.activityTestRule.activity.getString(R.string.preferenceKeyExperimentalFeatures))
        writeTo(R.id.inputValue, "this, that,    other")
        clickDialogPositiveButton()
        assertContains(R.id.effectiveConfiguration, "\"experimentalFeatures\" : [ \"other\", \"that\", \"this\" ]")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun configurationManagementCanEditAStringType() {
        clickOn(R.string.configurationManagement)
        openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)

        clickOn(R.string.preferencesEditor)
        writeTo(R.id.inputKey, baristaRule.activityTestRule.activity.getString(R.string.preferenceKeyModeId))
        writeTo(R.id.inputValue, "0")
        clickDialogPositiveButton()

        openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
        clickOn(R.string.preferencesEditor)
        writeTo(R.id.inputKey, baristaRule.activityTestRule.activity.getString(R.string.preferenceKeyHost))
        writeTo(R.id.inputValue, "example.com")
        clickDialogPositiveButton()

        assertContains(R.id.effectiveConfiguration, "\"host\" : \"example.com\"")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun configurationManagementCanEditABooleanType() {
        clickOn(R.string.configurationManagement)
        openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
        clickOn(R.string.preferencesEditor)
        writeTo(R.id.inputKey, baristaRule.activityTestRule.activity.getString(R.string.preferenceKeyRemoteCommand))
        writeTo(R.id.inputValue, "false")
        clickDialogPositiveButton()

        assertContains(R.id.effectiveConfiguration, "\"cmd\" : false")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun settingSimpleHTTPConfigSettingsCanBeExported() {

        clickOnAndWait(R.string.preferencesServer)
        clickOnAndWait(R.string.mode_heading)
        clickOnAndWait(R.string.mode_http_private_label)
        clickDialogPositiveButton()
        clickOnAndWait(R.string.preferencesHost)
        writeTo(R.id.url, "https://www.example.com:8080/")
        clickDialogPositiveButton()
        clickOnAndWait(R.string.preferencesIdentification)
        writeTo(R.id.username, "testUsername")
        writeTo(R.id.password, "testPassword")
        writeTo(R.id.deviceId, "testDeviceId")
        writeTo(R.id.trackerId, "t1")
        clickDialogPositiveButton()
        clickBack()


        clickOnAndWait(R.string.preferencesReporting)
        clickOnAndWait(R.string.preferencesPubExtendedData)
        clickBack()

        clickOnAndWait(R.string.preferencesNotification)
        clickOnAndWait(R.string.preferencesNotificationEvents)
        clickBack()

        // This is an ugly hack, but there's some race conditions on underpowered hardware
        // causing the test to move on before the view has been fully built/rendered.

        clickOnAndWait(R.string.preferencesAdvanced)
        clickOnAndWait(R.string.preferencesRemoteCommand)
        clickOnAndWait(R.string.preferencesIgnoreInaccurateLocations)

        writeTo(android.R.id.edit, "950")

        clickDialogPositiveButton()

        clickOnAndWait(R.string.preferencesLocatorInterval)

        writeTo(android.R.id.edit, "123")

        clickDialogPositiveButton()

        scrollToText(R.string.preferencesMoveModeLocatorInterval)
        clickOnAndWait(R.string.preferencesMoveModeLocatorInterval)

        writeTo(android.R.id.edit, "5")

        clickDialogPositiveButton()

        scrollToText(R.string.preferencesAutostart)
        clickOnAndWait(R.string.preferencesAutostart)

        /* TODO: Espresso doesn't work with dropdowns. Which is a bit silly. Restore this section once fixed */
//        scrollToText(R.string.preferencesReverseGeocodeProvider)
//        clickOnAndWait(R.string.preferencesReverseGeocodeProvider)
//        val something = onView(allOf(withId(android.R.id.text1), withText("OpenCage")))
//
//        something.perform(longClick())
//
//        scrollToText(R.string.preferencesOpencageGeocoderApiKey)
//        clickOnAndWait(R.string.preferencesOpencageGeocoderApiKey)
//        writeTo(android.R.id.edit, "geocodeAPIKey")
//        clickDialogPositiveButton()

        clickBack()

        clickOnAndWait(R.string.configurationManagement)

        assertContains(R.id.effectiveConfiguration, "\"_type\" : \"configuration\"")
        assertContains(R.id.effectiveConfiguration, " \"waypoints\" : [ ]")

        assertContains(R.id.effectiveConfiguration, "\"url\" : \"https://www.example.com:8080/\"")
        assertContains(R.id.effectiveConfiguration, "\"username\" : \"testUsername\"")
        assertContains(R.id.effectiveConfiguration, "\"password\" : \"********\"")
        assertContains(R.id.effectiveConfiguration, "\"deviceId\" : \"testDeviceId\"")
        assertContains(R.id.effectiveConfiguration, "\"tid\" : \"t1\"")
        assertContains(R.id.effectiveConfiguration, "\"notificationEvents\" : false")
        assertContains(R.id.effectiveConfiguration, "\"pubExtendedData\" : false")
        assertContains(R.id.effectiveConfiguration, "\"ignoreInaccurateLocations\" : 950")
        assertContains(R.id.effectiveConfiguration, "\"locatorInterval\" : 123")
        assertContains(R.id.effectiveConfiguration, "\"moveModeLocatorInterval\" : 5")
        assertContains(R.id.effectiveConfiguration, "\"autostartOnBoot\" : false")
//        assertContains(R.id.effectiveConfiguration, "\"reverseGeocodeProvider\" : \"OpenCage\"`")
//        assertContains(R.id.effectiveConfiguration, "\"opencageApiKey\" : \"geocodeAPIKey\"")

        // Make sure that the MQTT-specific settings aren't present
        assertNotContains(R.id.effectiveConfiguration, "\"host\"")
        assertNotContains(R.id.effectiveConfiguration, "\"port\"")
        assertNotContains(R.id.effectiveConfiguration, "\"pubQos\"")
        assertNotContains(R.id.effectiveConfiguration, "\"subQos\"")
        assertNotContains(R.id.effectiveConfiguration, "\"info\"")
        assertNotContains(R.id.effectiveConfiguration, "\"tlsCaCrt\"")
        assertNotContains(R.id.effectiveConfiguration, "\"tls\"")
        assertNotContains(R.id.effectiveConfiguration, "\"mqttProtocolLevel\"")
        assertNotContains(R.id.effectiveConfiguration, "\"subTopic\"")
        assertNotContains(R.id.effectiveConfiguration, "\"pubTopicBase\"")
        assertNotContains(R.id.effectiveConfiguration, "\"clientId\"")
    }

    private fun scrollToText(textResource: Int) {
        onView(withId(androidx.preference.R.id.recycler_view))
                .perform(actionOnItem<RecyclerView.ViewHolder>(
                        hasDescendant(withText(textResource)), scrollTo()))
    }
}