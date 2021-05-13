package org.owntracks.android.ui.map

import android.Manifest
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.schibsted.spain.barista.assertion.BaristaClickableAssertions.assertClickable
import com.schibsted.spain.barista.assertion.BaristaDrawerAssertions.assertDrawerIsClosed
import com.schibsted.spain.barista.assertion.BaristaEnabledAssertions.assertEnabled
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickBack
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import com.schibsted.spain.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.schibsted.spain.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.schibsted.spain.barista.interaction.BaristaEditTextInteractions.writeTo
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions.sleep
import com.schibsted.spain.barista.interaction.PermissionGranter
import com.schibsted.spain.barista.rule.BaristaRule
import com.schibsted.spain.barista.rule.flaky.AllowFlaky
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.owntracks.android.R
import org.owntracks.android.ScreenshotTakingOnTestEndRule
import org.owntracks.android.e2e.doWelcomeProcess
import org.owntracks.android.support.Preferences
import org.owntracks.android.ui.clickOnAndWait

@LargeTest
@RunWith(AndroidJUnit4::class)
/* TODO the android orchestrator doesn't work with coverage, so until it does we need to run tests in order */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GMSMapActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(MapActivity::class.java)

    private val screenshotRule = ScreenshotTakingOnTestEndRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(baristaRule.activityTestRule)
        .around(screenshotRule)


    @Test
    @Ignore("With the current test runner, permissions carry over between tests. So we need to figure out a way of running this one first.")
    @AllowFlaky(attempts = 1)
    fun aaa_onFirstStartTheWelcomeActivityIsLoadedAndCanBeClickedThroughToTheEnd() {
        baristaRule.launchActivity()
        assertDisplayed(R.string.welcome_heading)
        assertDisplayed(R.string.welcome_description)

        assertDisplayed(R.id.btn_next)
        assertClickable(R.id.btn_next)

        assertNotDisplayed(R.id.done)

        clickOn(R.id.btn_next)
        clickOn(R.id.btn_next)

        assertNotDisplayed(R.id.btn_next)

        assertDisplayed(R.id.fix_permissions_button)
        assertClickable(R.id.fix_permissions_button)

        clickOn(R.id.fix_permissions_button)
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
        sleep(2000)
        assertDisplayed(R.id.btn_next)

        clickOn(R.id.btn_next)

        assertDisplayed(R.string.done_heading)

        assertDisplayed(R.id.done)
        assertClickable(R.id.done)
        assertNotDisplayed(R.id.btn_next)
    }

    @Test
    @AllowFlaky(attempts = 1)
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
    @AllowFlaky(attempts = 1)
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
    @AllowFlaky(attempts = 1)
    fun welcomeActivityShouldNotRunWhenFirstStartPreferencesSet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(context.getString(R.string.preferenceKeyFirstStart), false)
            .putBoolean(context.getString(R.string.preferenceKeySetupNotCompleted), false)
            .apply()
        baristaRule.launchActivity()
        assertDisplayed(R.id.google_map_view)

    }

    @Test
    @AllowFlaky(attempts = 1)
    fun enablingOSMMapSwitchesFromGMSMapToOSMMap() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(context.getString(R.string.preferenceKeyFirstStart), false)
            .putBoolean(context.getString(R.string.preferenceKeySetupNotCompleted), false)
            .apply()
        baristaRule.launchActivity()
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
    @AllowFlaky(attempts = 1)
    fun modeButtonOnMapActivityCyclesThroughModes() {
        baristaRule.launchActivity()
        doWelcomeProcess()

        assertDisplayed(R.id.menu_monitoring)
    }
}
