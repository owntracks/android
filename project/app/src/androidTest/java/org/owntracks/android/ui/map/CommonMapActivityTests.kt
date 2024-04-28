package org.owntracks.android.ui.map

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.adevinta.android.barista.assertion.BaristaDrawerAssertions.assertDrawerIsClosed
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.GPSMockDeviceLocation
import org.owntracks.android.testutils.JustThisTestPlease
import org.owntracks.android.testutils.MockDeviceLocation
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.matchers.withActionIconDrawable
import org.owntracks.android.testutils.reportLocationFromMap
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.clickOnDrawerAndWait

@MediumTest
@RunWith(AndroidJUnit4::class)
class CommonMapActivityTests :
    TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    MockDeviceLocation by GPSMockDeviceLocation() {

  @Test
  fun monitoringModeButtonShowsDialogAndAllowsUsToSelectQuietMode() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()
    assertDisplayed(R.id.menu_monitoring)
    clickOn(R.id.menu_monitoring)

    assertDisplayed(R.id.fabMonitoringModeManual)
    assertDisplayed(R.id.fabMonitoringModeQuiet)
    assertDisplayed(R.id.fabMonitoringModeSignificantChanges)
    assertDisplayed(R.id.fabMonitoringModeMove)
    clickOnAndWait(R.id.fabMonitoringModeQuiet)
    onView(withId(R.id.menu_monitoring))
        .check(matches(withActionIconDrawable(R.drawable.ic_baseline_stop_36)))
  }

  @Test
  fun monitoringModeButtonShowsDialogAndAllowsUsToSelectManualMode() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()
    assertDisplayed(R.id.menu_monitoring)
    clickOn(R.id.menu_monitoring)

    assertDisplayed(R.id.fabMonitoringModeManual)
    assertDisplayed(R.id.fabMonitoringModeQuiet)
    assertDisplayed(R.id.fabMonitoringModeSignificantChanges)
    assertDisplayed(R.id.fabMonitoringModeMove)
    clickOnAndWait(R.id.fabMonitoringModeManual)
    onView(withId(R.id.menu_monitoring))
        .check(matches(withActionIconDrawable(R.drawable.ic_baseline_pause_36)))
  }

  @Test
  fun monitoringModeButtonShowsDialogAndAllowsUsToSelectSignificantMode() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()
    assertDisplayed(R.id.menu_monitoring)
    clickOn(R.id.menu_monitoring)

    assertDisplayed(R.id.fabMonitoringModeManual)
    assertDisplayed(R.id.fabMonitoringModeQuiet)
    assertDisplayed(R.id.fabMonitoringModeSignificantChanges)
    assertDisplayed(R.id.fabMonitoringModeMove)
    clickOnAndWait(R.id.fabMonitoringModeSignificantChanges)
    onView(withId(R.id.menu_monitoring))
        .check(matches(withActionIconDrawable(R.drawable.ic_baseline_play_arrow_36)))
  }

  @Test
  fun monitoringModeButtonShowsDialogAndAllowsUsToSelectMoveMode() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()
    assertDisplayed(R.id.menu_monitoring)
    clickOn(R.id.menu_monitoring)

    assertDisplayed(R.id.fabMonitoringModeManual)
    assertDisplayed(R.id.fabMonitoringModeQuiet)
    assertDisplayed(R.id.fabMonitoringModeSignificantChanges)
    assertDisplayed(R.id.fabMonitoringModeMove)
    clickOnAndWait(R.id.fabMonitoringModeMove)
    onView(withId(R.id.menu_monitoring))
        .check(matches(withActionIconDrawable(R.drawable.ic_step_forward_2)))
  }

  @Test
  fun mapCanSwitchToNightMode() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()
    assertDrawerIsClosed()

    openDrawer()

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
  @JustThisTestPlease
  fun regionsCanBeDrawnOnMap() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()
    initializeMockLocationProvider(app)
    reportLocationFromMap(app.mockLocationIdlingResource) { setMockLocation(51.0, 0.0) }

    openDrawer()
    clickOnDrawerAndWait(R.string.title_activity_waypoints)

    clickOnAndWait(R.id.add)
    writeTo(R.id.description, "testwaypoint")
    writeTo(R.id.latitude, "51.0")
    writeTo(R.id.longitude, "0")
    writeTo(R.id.radius, "250")

    clickOnAndWait(R.id.save)

    openDrawer()

    clickOnDrawerAndWait(R.string.title_activity_preferences)
    clickOnAndWait(R.string.title_activity_map)
    clickOnAndWait(R.string.preferencesShowWaypointsOnMap)
    openDrawer()
    clickOnDrawerAndWait(R.string.title_activity_map)
    assertDisplayed(R.id.mapCoordinatorLayout)
  }
}
