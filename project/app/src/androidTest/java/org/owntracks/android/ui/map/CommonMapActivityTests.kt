package org.owntracks.android.ui.map

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaDrawerAssertions.assertDrawerIsClosed
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions
import com.adevinta.android.barista.interaction.PermissionGranter
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.*
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.clickOnDrawerAndWait

@LargeTest
@RunWith(AndroidJUnit4::class)
class CommonMapActivityTests :
    TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    MockDeviceLocation by GPSMockDeviceLocation() {
    @After
    fun removeMockLocationProvider() {
        unInitializeMockLocationProvider()
    }

    @Test
    fun monitoringModeButtonShowsDialogAndAllowsUsToSelectQuietMode() {
        setNotFirstStartPreferences()
        launchActivity()
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
        assertDisplayed(R.id.menu_monitoring)
        clickOn(R.id.menu_monitoring)

        assertDisplayed(R.id.fabMonitoringModeManual)
        assertDisplayed(R.id.fabMonitoringModeQuiet)
        assertDisplayed(R.id.fabMonitoringModeSignificantChanges)
        assertDisplayed(R.id.fabMonitoringModeMove)
        clickOnAndWait(R.id.fabMonitoringModeQuiet)
        onView(withId(R.id.menu_monitoring)).check(matches(withActionIconDrawable(R.drawable.ic_baseline_stop_36)))
    }

    @Test
    fun monitoringModeButtonShowsDialogAndAllowsUsToSelectManualMode() {
        setNotFirstStartPreferences()
        launchActivity()
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
        assertDisplayed(R.id.menu_monitoring)
        clickOn(R.id.menu_monitoring)

        assertDisplayed(R.id.fabMonitoringModeManual)
        assertDisplayed(R.id.fabMonitoringModeQuiet)
        assertDisplayed(R.id.fabMonitoringModeSignificantChanges)
        assertDisplayed(R.id.fabMonitoringModeMove)
        clickOnAndWait(R.id.fabMonitoringModeManual)
        onView(withId(R.id.menu_monitoring)).check(matches(withActionIconDrawable(R.drawable.ic_baseline_pause_36)))
    }

    @Test
    fun monitoringModeButtonShowsDialogAndAllowsUsToSelectSignificantMode() {
        setNotFirstStartPreferences()
        launchActivity()
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
        assertDisplayed(R.id.menu_monitoring)
        clickOn(R.id.menu_monitoring)

        assertDisplayed(R.id.fabMonitoringModeManual)
        assertDisplayed(R.id.fabMonitoringModeQuiet)
        assertDisplayed(R.id.fabMonitoringModeSignificantChanges)
        assertDisplayed(R.id.fabMonitoringModeMove)
        clickOnAndWait(R.id.fabMonitoringModeSignificantChanges)
        onView(withId(R.id.menu_monitoring)).check(matches(withActionIconDrawable(R.drawable.ic_baseline_play_arrow_36)))
    }

    @Test
    fun monitoringModeButtonShowsDialogAndAllowsUsToSelectMoveMode() {
        setNotFirstStartPreferences()
        launchActivity()
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
        assertDisplayed(R.id.menu_monitoring)
        clickOn(R.id.menu_monitoring)

        assertDisplayed(R.id.fabMonitoringModeManual)
        assertDisplayed(R.id.fabMonitoringModeQuiet)
        assertDisplayed(R.id.fabMonitoringModeSignificantChanges)
        assertDisplayed(R.id.fabMonitoringModeMove)
        clickOnAndWait(R.id.fabMonitoringModeMove)
        onView(withId(R.id.menu_monitoring)).check(matches(withActionIconDrawable(R.drawable.ic_step_forward_2)))
    }

    @Test
    fun mapCanSwitchToNightMode() {
        setNotFirstStartPreferences()
        launchActivity()
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
        assertDrawerIsClosed()

        BaristaDrawerInteractions.openDrawer()

        assertDisplayed(R.string.title_activity_preferences)
        BaristaEnabledAssertions.assertEnabled(R.string.title_activity_preferences)

        clickOnDrawerAndWait(R.string.title_activity_preferences)

        clickOn(R.string.preferencesTheme)

        clickOn("Always in dark theme")

        openDrawer()
        clickOnDrawerAndWait(R.string.title_activity_map)
        assertDisplayed(R.id.mapCoordinatorLayout)
    }

    @Test
    fun regionsCanBeDrawnOnMap() {
        setNotFirstStartPreferences()
        launchActivity()
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
        initializeMockLocationProvider(baristaRule.activityTestRule.activity.applicationContext)
        baristaRule.activityTestRule.activity.locationIdlingResource.with {
            setMockLocation(51.0, 0.0)
            clickOnAndWait(R.id.fabMyLocation)
        }

        BaristaDrawerInteractions.openDrawer()

        clickOnDrawerAndWait(R.string.title_activity_regions)

        clickOnAndWait(R.id.add)
        BaristaEditTextInteractions.writeTo(R.id.description, "testregion")
        BaristaEditTextInteractions.writeTo(R.id.latitude, "51.0")
        BaristaEditTextInteractions.writeTo(R.id.longitude, "0")
        BaristaEditTextInteractions.writeTo(R.id.radius, "250")

        clickOnAndWait(R.id.save)

        openDrawer()

        clickOnDrawerAndWait(R.string.title_activity_preferences)
        clickOnAndWait(R.string.title_activity_map)
        clickOnAndWait(R.string.preferencesShowRegionsOnMap)
        openDrawer()
        clickOnDrawerAndWait(R.string.title_activity_map)
        assertDisplayed(R.id.mapCoordinatorLayout)
    }
}
