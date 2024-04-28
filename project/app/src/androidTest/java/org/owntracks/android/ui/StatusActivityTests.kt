package org.owntracks.android.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.ui.status.StatusActivity

@MediumTest
@RunWith(AndroidJUnit4::class)
class StatusActivityTests : TestWithAnActivity<StatusActivity>(StatusActivity::class.java) {
  @Test
  fun statusActivityShowsEndpointState() {
    assertDisplayed(R.string.status_endpoint_state_hint)
  }

  @Test
  fun statusActivityShowsLogsLauncher() {
    assertDisplayed(R.string.viewLogs)
  }

  @Test
  fun whenClickingBatteryOptimizationWhitelistThenDialogIsShown() {
    assertDisplayed(R.string.status_battery_optimization_whitelisted_hint)
    clickOnAndWait(R.string.status_battery_optimization_whitelisted_hint)
    assertDisplayed(R.string.batteryOptimizationWhitelistDialogTitle)
    assertDisplayed(R.string.batteryOptimizationWhitelistDialogMessage)
    assertDisplayed(R.string.batteryOptimizationWhitelistDialogButtonLabel)
  }
}
