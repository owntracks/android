package org.owntracks.android.ui

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
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
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions.sleep
import com.schibsted.spain.barista.rule.BaristaRule
import com.schibsted.spain.barista.rule.flaky.AllowFlaky
import org.hamcrest.CoreMatchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.ScreenshotTakingOnTestEndRule
import org.owntracks.android.ui.preferences.PreferencesActivity

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
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(baristaRule.activityTestRule.activity.getString(R.string.documentationUrl))))
        Intents.release()
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun twitterLinkOpensSite() {
        try {
            Intents.init()
            clickOn(R.string.preferencesInfo)
            clickOn(R.string.preferencesTwitter)
            intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(baristaRule.activityTestRule.activity.getString(R.string.twitterUrl))))
        } finally {
            Intents.release()
        }
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun sourceLinkOpensSite() {
        try {
            Intents.init()
            clickOn(R.string.preferencesInfo)
            clickOn(R.string.preferencesRepository)
            intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(baristaRule.activityTestRule.activity.getString(R.string.repoUrl))))
        } finally {
            Intents.release()
        }
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun librariesLinkListsLibraries() {
        try {
            Intents.init()
            clickOn(R.string.preferencesInfo)
            clickOn(R.string.preferencesLicenses)
            assertDisplayed(R.string.preferencesLicenses)
        } finally {
            Intents.release()
        }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun settingSimpleHTTPConfigSettingsCanBeExported() {
        clickOn(R.string.preferencesServer)
        clickOn(R.string.mode_heading)
        clickOn(R.string.mode_http_private_label)
        clickDialogPositiveButton()
        clickOn(R.string.preferencesHost)
        writeTo(R.id.url, "https://www.example.com:8080/")
        clickDialogPositiveButton()
        clickOn(R.string.preferencesIdentification)
        writeTo(R.id.username, "testUsername")
        writeTo(R.id.password, "testPassword")
        writeTo(R.id.deviceId, "testDeviceId")
        writeTo(R.id.trackerId, "t1")
        clickDialogPositiveButton()
        clickBack()

        clickOn(R.string.preferencesReporting)
        clickOn(R.string.preferencesPubExtendedData)
        clickBack()

        clickOn(R.string.preferencesNotification)
        clickOn(R.string.preferencesNotificationEvents)
        clickBack()

        // This is an ugly hack, but there's some race conditions on underpowered hardware
        // causing the test to move on before the view has been fully built/rendered.
        clickOn(R.string.preferencesAdvanced)
        sleep(100)
        clickOn(R.string.preferencesRemoteCommand)
        sleep(100)
        clickOn(R.string.preferencesIgnoreInaccurateLocations)
        sleep(100)
        writeTo(android.R.id.edit, "950")
        sleep(100)
        clickDialogPositiveButton()
        sleep(100)
        clickOn(R.string.preferencesLocatorInterval)
        sleep(100)
        writeTo(android.R.id.edit, "123")
        sleep(100)
        clickDialogPositiveButton()
        sleep(100)
        clickOn(R.string.preferencesMoveModeLocatorInterval)
        sleep(100)
        writeTo(android.R.id.edit, "5")
        sleep(100)
        clickDialogPositiveButton()

        scrollToText(R.string.preferencesAutostart)
        clickOn(R.string.preferencesAutostart)

        scrollToText(R.string.preferencesGeocode)
        clickOn(R.string.preferencesGeocode)

        scrollToText(R.string.preferencesOpencageGeocoderApiKey)
        clickOn(R.string.preferencesOpencageGeocoderApiKey)
        writeTo(android.R.id.edit, "geocodeAPIKey")
        clickDialogPositiveButton()
        sleep(100)
        clickBack()
        sleep(100)

        clickOn(R.string.configurationManagement)

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
        assertContains(R.id.effectiveConfiguration, "\"geocodeEnabled\" : false")
        assertContains(R.id.effectiveConfiguration, "\"opencageApiKey\" : \"geocodeAPIKey\"")

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