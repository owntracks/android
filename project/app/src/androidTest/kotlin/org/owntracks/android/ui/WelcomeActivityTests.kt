package org.owntracks.android.ui

import android.Manifest
import androidx.test.espresso.Espresso.pressBack
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.PermissionGranter.allowPermissionsIfNeeded
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.doIfViewNotVisible
import org.owntracks.android.testutils.getCurrentActivity
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.welcome.WelcomeActivity

@MediumTest
@HiltAndroidTest
class WelcomeActivityTests : TestWithAnActivity<WelcomeActivity>() {

  @Test
  fun welcome_activity_starts_with_intro_fragment() {
    assertDisplayed(R.string.welcome_heading)
    assertDisplayed(R.string.welcome_description)
    assertDisplayed(R.id.btn_next)
  }

  @Test
  fun welcome_activity_shows_connection_setup_details() {
    // Intro fragment
    clickOn(R.id.btn_next)
    // Connection setup fragment
    assertDisplayed(R.string.welcome_connection_setup_title)
    assertDisplayed(R.string.welcome_connection_setup_description)
    assertDisplayed(R.id.btn_next)
  }

  @SdkSuppress(minSdkVersion = 34)
  @Test
  fun welcome_activity_starts_the_map_activity_when_done() {
    // Intro fragment
    clickOn(R.id.btn_next)
    // Connection setup fragment
    clickOn(R.id.btn_next)
    // Location Permission fragment
    doIfViewNotVisible(R.id.btn_next) {
      R.id.ui_fragment_welcome_location_permissions_request.run {
        assertDisplayed(this)
        clickOn(this)
      }
      allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    clickOn(R.id.btn_next)
    // Notification permission fragment
    doIfViewNotVisible(R.id.btn_next) {
      R.id.ui_fragment_welcome_notification_permissions_request.run {
        assertDisplayed(this)
        clickOn(this)
      }
      allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)
    }
    clickOn(R.id.btn_next)
    clickOn(R.id.btn_done)
    assertTrue(getCurrentActivity() is MapActivity)
  }

  @SdkSuppress(minSdkVersion = 29)
  @Test
  fun welcome_activity_prompts_for_background_location_permission() {
    clickOn(R.id.btn_next)
    clickOn(R.id.btn_next)

    // Location permissions fragment
    assertDisplayed(R.string.welcome_location_permission_description)
    doIfViewNotVisible(R.id.btn_next) {
      R.id.ui_fragment_welcome_location_permissions_request.run {
        assertDisplayed(this)
        clickOn(this)
      }
      allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    assertDisplayed(R.id.btn_next)
    assertDisplayed(R.id.ui_fragment_welcome_location_background_permissions_request)
  }

  @SdkSuppress(maxSdkVersion = 28)
  @Test
  fun welcome_activity_doesnt_prompt_for_background_location_permission() {
    clickOn(R.id.btn_next)
    clickOn(R.id.btn_next)

    // Location permissions fragment
    assertDisplayed(R.string.welcome_location_permission_description)
    doIfViewNotVisible(R.id.btn_next) {
      R.id.ui_fragment_welcome_location_permissions_request.run {
        assertDisplayed(this)
        clickOn(this)
      }
      allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    assertDisplayed(R.id.btn_next)
    assertNotDisplayed(R.id.ui_fragment_welcome_location_background_permissions_request)
  }

  @SdkSuppress(minSdkVersion = 34)
  @Test
  fun welcome_activity_displays_correct_fragments_with_notification_permissions() {
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
      assertDisplayed(R.id.ui_fragment_welcome_location_background_permissions_request)
      clickOn(this)
    }

    // Notification permissions fragment
    assertDisplayed(R.string.welcome_notification_permission_description)
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
  fun welcome_activity_displays_correct_fragments() {
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

  @Test
  fun welcome_activity_can_be_swiped_back_to_start() {
    clickOn(R.id.btn_next)
    pressBack()
    assertDisplayed(R.string.welcome_heading)
  }
}
