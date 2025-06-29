package org.owntracks.android.ui

import androidx.test.filters.MediumTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.ui.status.StatusActivity

@MediumTest
@HiltAndroidTest
class StatusActivityTests : TestWithAnActivity<StatusActivity>() {
  @get:Rule(order = 0) override var hiltRule = HiltAndroidRule(this)

  @Test
  fun status_activity_shows_endpoint_state() {
    assertDisplayed(R.string.status_endpoint_state_hint)
  }

  @Test
  fun status_activity_shows_logs_launcher() {
    assertDisplayed(R.string.viewLogs)
  }

  @Test
  fun when_clicking_battery_optimization_whitelist_then_dialog_is_shown() {
    assertDisplayed(R.string.status_battery_optimization_whitelisted_hint)
    clickOn(R.string.status_battery_optimization_whitelisted_hint)
    assertDisplayed(R.string.batteryOptimizationWhitelistDialogTitle)
    assertDisplayed(R.string.batteryOptimizationWhitelistDialogMessage)
    assertDisplayed(R.string.batteryOptimizationWhitelistDialogButtonLabel)
  }
}
