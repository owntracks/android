package org.owntracks.android.ui

import android.Manifest
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.PermissionGranter.allowPermissionsIfNeeded
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.doIfViewNotVisible
import org.owntracks.android.ui.welcome.WelcomeActivity

@RunWith(AndroidJUnit4::class)
class WelcomeActivityTests : TestWithAnActivity<WelcomeActivity>(WelcomeActivity::class.java) {

  @SdkSuppress(minSdkVersion = 34)
  @Test
  fun welcomeActivityDisplaysCorrectFragmentsWithNotificationPermissions() {
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
    assertDisplayed(R.id.ui_fragment_welcome_location_permissions_message)
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

    // Notification permissions fragment
    assertDisplayed(R.id.ui_fragment_welcome_notification_permissions_message)
    doIfViewNotVisible(R.id.btn_next) {
      R.id.ui_fragment_welcome_notification_permissions_request.run {
        assertDisplayed(this)
        clickOn(this)
      }
      allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)
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

  @Test
  @SdkSuppress(minSdkVersion = 24, maxSdkVersion = 33)
  fun welcomeActivityDisplaysCorrectFragments() {
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
    assertDisplayed(R.id.ui_fragment_welcome_location_permissions_message)
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

  @Test
  fun welcomeActivityCanBeSwipedBackToStart() {
    clickOn(R.id.btn_next)
    pressBack()
    assertDisplayed(R.string.welcome_heading)
  }
}
