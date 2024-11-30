package org.owntracks.android.ui.map

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.MediumTest
import com.adevinta.android.barista.assertion.BaristaDrawerAssertions.assertDrawerIsClosed
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.testutils.JustThisTestPlease
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.addWaypoint
import org.owntracks.android.testutils.clickOnDrawerAndWait
import org.owntracks.android.testutils.di.setLocation
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.matchers.withActionIconDrawable
import org.owntracks.android.testutils.setNotFirstStartPreferences

@MediumTest
@HiltAndroidTest
@JustThisTestPlease
class CommonMapActivityTests : TestWithAnActivity<MapActivity>(false) {

  @Test
  fun monitoring_mode_button_shows_dialog_and_allows_us_to_select_quiet_mode() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()
    assertDisplayed(R.id.menu_monitoring)
    clickOn(R.id.menu_monitoring)

    assertDisplayed(R.id.fabMonitoringModeManual)
    assertDisplayed(R.id.fabMonitoringModeQuiet)
    assertDisplayed(R.id.fabMonitoringModeSignificantChanges)
    assertDisplayed(R.id.fabMonitoringModeMove)
    clickOn(R.id.fabMonitoringModeQuiet)
    onView(withId(R.id.menu_monitoring))
        .check(matches(withActionIconDrawable(R.drawable.ic_baseline_stop_36)))
  }

  @Test
  fun monitoring_mode_button_shows_dialog_and_allows_us_to_select_manual_mode() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()
    assertDisplayed(R.id.menu_monitoring)
    clickOn(R.id.menu_monitoring)

    assertDisplayed(R.id.fabMonitoringModeManual)
    assertDisplayed(R.id.fabMonitoringModeQuiet)
    assertDisplayed(R.id.fabMonitoringModeSignificantChanges)
    assertDisplayed(R.id.fabMonitoringModeMove)
    clickOn(R.id.fabMonitoringModeManual)
    onView(withId(R.id.menu_monitoring))
        .check(matches(withActionIconDrawable(R.drawable.ic_baseline_pause_36)))
  }

  @Test
  fun monitoring_mode_button_shows_dialog_and_allows_us_to_select_significant_mode() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()
    assertDisplayed(R.id.menu_monitoring)
    clickOn(R.id.menu_monitoring)

    assertDisplayed(R.id.fabMonitoringModeManual)
    assertDisplayed(R.id.fabMonitoringModeQuiet)
    assertDisplayed(R.id.fabMonitoringModeSignificantChanges)
    assertDisplayed(R.id.fabMonitoringModeMove)
    clickOn(R.id.fabMonitoringModeSignificantChanges)
    onView(withId(R.id.menu_monitoring))
        .check(matches(withActionIconDrawable(R.drawable.ic_baseline_play_arrow_36)))
  }

  @Test
  fun monitoring_mode_button_shows_dialog_and_allows_us_to_select_move_mode() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()
    assertDisplayed(R.id.menu_monitoring)
    clickOn(R.id.menu_monitoring)

    assertDisplayed(R.id.fabMonitoringModeManual)
    assertDisplayed(R.id.fabMonitoringModeQuiet)
    assertDisplayed(R.id.fabMonitoringModeSignificantChanges)
    assertDisplayed(R.id.fabMonitoringModeMove)
    clickOn(R.id.fabMonitoringModeMove)
    onView(withId(R.id.menu_monitoring))
        .check(matches(withActionIconDrawable(R.drawable.ic_step_forward_2)))
  }

  @Test
  fun map_can_switch_to_night_mode() {
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
  fun regions_can_be_drawn_on_map() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()

    reportLocationFromMap(mockLocationIdlingResource) {
      mockLocationProviderClient.setLocation(51.0, 0.0)
    }

    openDrawer()
    clickOnDrawerAndWait(R.string.title_activity_waypoints)

    addWaypoint("testwaypoint", "51.0", "0", "250")

    openDrawer()

    clickOnDrawerAndWait(R.string.title_activity_preferences)
    clickOn(R.string.title_activity_map)
    clickOn(R.string.preferencesShowWaypointsOnMap)
    openDrawer()
    clickOnDrawerAndWait(R.string.title_activity_map)
    assertDisplayed(R.id.mapCoordinatorLayout)
  }
}
