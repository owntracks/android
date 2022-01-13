package org.owntracks.android.ui.map

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import com.adevinta.android.barista.interaction.PermissionGranter
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.support.Preferences
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.setNotFirstStartPreferences

@LargeTest
@RunWith(AndroidJUnit4::class)
class OSSMapActivityTests : TestWithAnActivity<MapActivity>(MapActivity::class.java, false) {
    @Test
    fun welcomeActivityShouldNotRunWhenFirstStartPreferencesSet() {
        setNotFirstStartPreferences()
        launchActivity()
        PermissionGranter.allowPermissionsIfNeeded(ACCESS_FINE_LOCATION)
        assertDisplayed(R.id.osm_map_view)
    }

    @Test
    fun mapActivityShouldPromptForLocationServicesOnFirstTime() {
        try {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put secure location_mode 0")
                .close()
            setNotFirstStartPreferences()
            launchActivity()
            PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
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
            setNotFirstStartPreferences()
            PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
                .edit()
                .putBoolean(Preferences.preferenceKeyUserDeclinedEnableLocationServices, true)
                .apply()
            launchActivity()
            PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
            assertNotExist(R.string.deviceLocationDisabledDialogTitle)
            assertDisplayed(R.id.osm_map_view)
        } finally {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put secure location_mode 3")
                .close()
        }
    }
}
