package org.owntracks.android.ui.map

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.PermissionGranter
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.testutils.withActionIconDrawable
import org.owntracks.android.ui.clickOnAndWait


@LargeTest
@RunWith(AndroidJUnit4::class)
class CommonMapActivityTests : TestWithAnActivity<MapActivity>(MapActivity::class.java, false) {
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
}