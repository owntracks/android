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
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.disableDeviceLocation
import org.owntracks.android.testutils.enableDeviceLocation
import org.owntracks.android.testutils.grantMapActivityPermissions
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
            disableDeviceLocation()
            setNotFirstStartPreferences()
            launchActivity()
            grantMapActivityPermissions()
            assertDisplayed(R.string.deviceLocationDisabledDialogTitle)
            clickDialogNegativeButton()
            assertDisplayed(R.id.osm_map_view)
        } finally {
            disableDeviceLocation()
        }
    }

    @Test
    fun mapActivityShouldNotPromptForLocationServicesIfPreviouslyDeclined() {
        try {
            disableDeviceLocation()
            setNotFirstStartPreferences()
            PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
                .edit()
                .putBoolean(Preferences::userDeclinedEnableLocationServices.name, true)
                .apply()
            launchActivity()
            grantMapActivityPermissions()
            assertNotExist(R.string.deviceLocationDisabledDialogTitle)
            assertDisplayed(R.id.osm_map_view)
        } finally {
            enableDeviceLocation()
        }
    }
}
