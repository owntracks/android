package org.owntracks.android.ui

import android.Manifest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.PermissionGranter.allowPermissionsIfNeeded
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.doIfViewNotVisible
import org.owntracks.android.ui.welcome.WelcomeActivity

@MediumTest
@HiltAndroidTest
class WelcomeActivityGmsTests : TestWithAnActivity<WelcomeActivity>() {

  @Test
  @SdkSuppress(minSdkVersion = 24, maxSdkVersion = 33)
  fun welcome_activity_displays_correct_fragments() {
    // On AOSP images without GMS, PlayFragment blocks navigation and the Done fragment is
    // unreachable. Skip rather than fail on such devices.
    Assume.assumeTrue(
        "Google Play Services not available",
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(
                InstrumentationRegistry.getInstrumentation().targetContext) ==
            ConnectionResult.SUCCESS)

    // Intro fragment
    assertDisplayed(R.string.welcome_heading)
    R.id.btn_next.run {
      assertDisplayed(this)
      clickOn(this)
    }

    // Connection setup fragment
    assertDisplayed(R.string.welcome_connection_setup_title)

    assertDisplayed(R.string.welcome_connection_setup_description)
    R.id.btn_next.run {
      assertDisplayed(this)
      clickOn(this)
    }

    // Location permissions fragment
    assertDisplayed(R.string.welcome_location_permission_description)
    doIfViewNotVisible(R.id.btn_next) {
      R.id.ui_fragment_welcome_location_permissions_request.run {
        assertDisplayed(this)
        clickOn(this)
      }
      allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    R.id.btn_next.run {
      assertDisplayed(this)
      clickOn(this)
    }

    // Done fragment
    assertDisplayed(R.string.done_heading)
    assertDisplayed(R.string.enjoy_description)
    assertDisplayed(R.string.welcome_finish_open_preferences_button_label)
    assertDisplayed(R.id.btn_done)
  }
}
