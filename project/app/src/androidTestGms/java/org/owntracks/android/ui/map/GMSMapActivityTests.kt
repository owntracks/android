package org.owntracks.android.ui.map

import android.Manifest
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingPolicies
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaDrawerAssertions.assertDrawerIsClosed
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertEnabled
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickBack
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.PermissionGranter
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.owntracks.android.R
import org.owntracks.android.ScreenshotTakingOnTestEndRule
import org.owntracks.android.e2e.doWelcomeProcess
import org.owntracks.android.e2e.setNotFirstStartPreferences
import org.owntracks.android.support.Preferences
import org.owntracks.android.ui.clickOnAndWait
import java.util.concurrent.TimeUnit


@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GMSMapActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(MapActivity::class.java)

    private val screenshotRule = ScreenshotTakingOnTestEndRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(baristaRule.activityTestRule)
        .around(screenshotRule)

    @Before
    fun setIdlingTimeout() {
        IdlingPolicies.setIdlingResourceTimeout(10, TimeUnit.SECONDS)
    }

    @Test
    @AllowFlaky
    fun statusActivityCanBeLaunchedFromMapActivityDrawer() {
        baristaRule.launchActivity()
        doWelcomeProcess()
        assertDrawerIsClosed()
        openDrawer()

        assertDisplayed(R.string.title_activity_status)
        assertEnabled(R.string.title_activity_status)

        clickOn(R.string.title_activity_status)

        arrayOf(
            R.string.status_battery_optimization_whitelisted_hint,
            R.string.status_endpoint_queue_hint,
            R.string.status_background_service_started_hint,
            R.string.status_endpoint_state_hint
        ).forEach { assertDisplayed(it) }
    }

    @Test
    @AllowFlaky
    fun preferencesActivityCanBeLaunchedFromMapActivityDrawer() {
        baristaRule.launchActivity()
        doWelcomeProcess()
        assertDrawerIsClosed()

        openDrawer()

        assertDisplayed(R.string.title_activity_preferences)
        assertEnabled(R.string.title_activity_preferences)

        clickOn(R.string.title_activity_preferences)

        arrayOf(
            R.string.preferencesServer,
            R.string.preferencesReporting,
            R.string.preferencesNotification,
            R.string.preferencesAdvanced,
            R.string.configurationManagement
        ).forEach {
            assertDisplayed(it)
        }
    }

    @Test
    @AllowFlaky
    fun welcomeActivityShouldNotRunWhenFirstStartPreferencesSet() {
        setNotFirstStartPreferences()
        baristaRule.launchActivity()
        assertDisplayed(R.id.google_map_view)
    }

    @Test
    @AllowFlaky
    fun enablingOSMMapSwitchesFromGMSMapToOSMMap() {
        setNotFirstStartPreferences()
        baristaRule.launchActivity()
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
        assertDisplayed(R.id.google_map_view)

        openDrawer()
        clickOnAndWait(R.string.title_activity_preferences)
        clickOnAndWait(R.string.configurationManagement)
        Espresso.openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
        clickOn(R.string.preferencesEditor)
        writeTo(
            R.id.inputKey,
            baristaRule.activityTestRule.activity.getString(R.string.preferenceKeyExperimentalFeatures)
        )
        writeTo(R.id.inputValue, Preferences.EXPERIMENTAL_FEATURE_USE_OSM_MAP)
        clickDialogPositiveButton()
        clickBack()
        clickBack()
        assertDisplayed(R.id.osm_map_view)
    }

    @Test
    @AllowFlaky
    fun modeButtonOnMapActivityCyclesThroughModes() {
        baristaRule.launchActivity()
        doWelcomeProcess()
        assertDisplayed(R.id.menu_monitoring)
    }

    @Test
    fun mapActivityShouldPromptForLocationServicesOnFirstTime() {
        try {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put secure location_mode 0")
                .close()
            setNotFirstStartPreferences()
            baristaRule.launchActivity()
            assertDisplayed(R.string.deviceLocationDisabledDialogTitle)
            clickDialogNegativeButton()
            assertDisplayed(R.id.google_map_view)
        } finally {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put secure location_mode 3")
                .close()
        }
    }

    @Test
    fun mapActivityShouldNotPromptForLocationServicesIfPreviouslyDeclined() {
        try {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put secure location_mode 0")
                .close()
            setNotFirstStartPreferences()
            PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
                .edit()
                .putBoolean(Preferences.preferenceKeyUserDeclinedEnableLocationServices, true)
                .apply()
            baristaRule.launchActivity()
            assertNotExist(R.string.deviceLocationDisabledDialogTitle)
            assertDisplayed(R.id.google_map_view)
        } finally {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put secure location_mode 3")
                .close()
        }
    }
}