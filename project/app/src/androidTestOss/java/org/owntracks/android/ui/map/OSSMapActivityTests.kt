package org.owntracks.android.ui.map

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import com.adevinta.android.barista.interaction.PermissionGranter
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.support.Preferences
import org.owntracks.android.testutils.rules.ScreenshotTakingOnTestEndRule

@LargeTest
@RunWith(AndroidJUnit4::class)
class OSSMapActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(MapActivity::class.java)

    private val screenshotRule = ScreenshotTakingOnTestEndRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(baristaRule.activityTestRule)
        .around(screenshotRule)

    @Test
    @AllowFlaky
    fun welcomeActivityShouldNotRunWhenFirstStartPreferencesSet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(context.getString(R.string.preferenceKeyFirstStart), false)
            .putBoolean(context.getString(R.string.preferenceKeySetupNotCompleted), false)
            .apply()
        baristaRule.launchActivity()
        PermissionGranter.allowPermissionsIfNeeded(ACCESS_FINE_LOCATION)
        assertDisplayed(R.id.osm_map_view)
    }

    @Test
    fun mapActivityShouldPromptForLocationServicesOnFirstTime() {
        try {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put secure location_mode 0")
                .close()
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(context.getString(R.string.preferenceKeyFirstStart), false)
                .putBoolean(context.getString(R.string.preferenceKeySetupNotCompleted), false)
                .apply()
            baristaRule.launchActivity()
            assertDisplayed(R.string.deviceLocationDisabledDialogTitle)
            clickDialogNegativeButton()
            assertDisplayed(R.id.osm_map_view)
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
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(context.getString(R.string.preferenceKeyFirstStart), false)
                .putBoolean(context.getString(R.string.preferenceKeySetupNotCompleted), false)
                .putBoolean(context.getString(R.string.preferenceKeySetupNotCompleted), false)
                .putBoolean(Preferences.preferenceKeyUserDeclinedEnableLocationServices, true)
                .apply()
            baristaRule.launchActivity()
            assertNotExist(R.string.deviceLocationDisabledDialogTitle)
            assertDisplayed(R.id.osm_map_view)
        } finally {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put secure location_mode 3")
                .close()
        }
    }
}
