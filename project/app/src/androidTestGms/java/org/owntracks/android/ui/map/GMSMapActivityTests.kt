package org.owntracks.android.ui.map

import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaDrawerAssertions.assertDrawerIsClosed
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertEnabled
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.disableDeviceLocation
import org.owntracks.android.testutils.enableDeviceLocation
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.setNotFirstStartPreferences

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GMSMapActivityTests : TestWithAnActivity<MapActivity>(MapActivity::class.java, false) {
    @Test
    fun statusActivityCanBeLaunchedFromMapActivityDrawer() {
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
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
    fun preferencesActivityCanBeLaunchedFromMapActivityDrawer() {
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
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
    fun welcomeActivityShouldNotRunWhenFirstStartPreferencesSet() {
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
        assertDisplayed(R.id.google_map_view)
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
            assertDisplayed(R.id.google_map_view)
        } finally {
            enableDeviceLocation()
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
            assertDisplayed(R.id.google_map_view)
        } finally {
            enableDeviceLocation()
        }
    }

    @Test
    fun mapCanSwitchLayerStyleToOsmAndBackAgain() {
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
        assertDisplayed(R.id.google_map_view)
        clickOn(R.id.fabMapLayers)
        clickOn(R.id.fabMapLayerOpenStreetMap)
        assertDisplayed(R.id.osm_map_view)
        clickOn(R.id.fabMapLayers)
        clickOn(R.id.fabMapLayerGoogleHybrid)
        assertDisplayed(R.id.google_map_view)
    }

    @Test
    fun mapStartsOnOSMMapIfPreferenceIsSelected() {
        setNotFirstStartPreferences()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(Preferences::mapLayerStyle.name, MapLayerStyle.OpenStreetMapNormal.name)
            .apply()
        launchActivity()
        grantMapActivityPermissions()
        assertDisplayed(R.id.osm_map_view)
    }
}
