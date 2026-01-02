package org.owntracks.android.ui

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.adevinta.android.barista.interaction.PermissionGranter.allowPermissionsIfNeeded
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.getCurrentActivity
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.welcome.WelcomeActivity
import org.owntracks.android.ui.welcome.WelcomeTestTags

@MediumTest
@HiltAndroidTest
class WelcomeActivityTests : TestWithAnActivity<WelcomeActivity>() {
  @get:Rule val composeTestRule = createAndroidComposeRule<WelcomeActivity>()

  @Test
  fun welcome_activity_starts_with_intro_fragment() {
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.welcome_heading))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.welcome_description))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).assertIsDisplayed()
  }

  @Test
  fun welcome_activity_shows_connection_setup_details() {
    // Intro fragment
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()
    // Connection setup fragment
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.welcome_connection_setup_title))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(
            composeTestRule.activity.getString(R.string.welcome_connection_setup_description))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).assertIsDisplayed()
  }

  @SdkSuppress(minSdkVersion = 34)
  @Test
  fun welcome_activity_starts_the_map_activity_when_done() {
    // Intro fragment
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()
    // Connection setup fragment
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()
    // Location Permission fragment
    composeTestRule.waitForIdle()
    try {
      composeTestRule.onNodeWithTag(WelcomeTestTags.LocationPermissionButton).assertIsDisplayed()
      composeTestRule.onNodeWithTag(WelcomeTestTags.LocationPermissionButton).performClick()
      allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
    } catch (e: AssertionError) {
      // Permission button not visible, already have permission
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()
    // Notification permission fragment
    composeTestRule.waitForIdle()
    try {
      composeTestRule
          .onNodeWithTag(WelcomeTestTags.NotificationPermissionButton)
          .assertIsDisplayed()
      composeTestRule.onNodeWithTag(WelcomeTestTags.NotificationPermissionButton).performClick()
      allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)
    } catch (e: AssertionError) {
      // Permission button not visible, already have permission
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()
    composeTestRule.onNodeWithTag(WelcomeTestTags.DoneButton).performClick()
    assertTrue(getCurrentActivity() is MapActivity)
  }

  @SdkSuppress(minSdkVersion = 29)
  @Test
  fun welcome_activity_prompts_for_background_location_permission() {
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()

    // Location permissions fragment
    composeTestRule
        .onNodeWithText(
            composeTestRule.activity.getString(R.string.welcome_location_permission_description))
        .assertIsDisplayed()
    composeTestRule.waitForIdle()
    try {
      composeTestRule.onNodeWithTag(WelcomeTestTags.LocationPermissionButton).assertIsDisplayed()
      composeTestRule.onNodeWithTag(WelcomeTestTags.LocationPermissionButton).performClick()
      allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
    } catch (e: AssertionError) {
      // Permission button not visible, already have permission
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(WelcomeTestTags.BackgroundLocationPermissionButton)
        .assertIsDisplayed()
  }

  @SdkSuppress(maxSdkVersion = 28)
  @Test
  fun welcome_activity_doesnt_prompt_for_background_location_permission() {
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()

    // Location permissions fragment
    composeTestRule
        .onNodeWithText(
            composeTestRule.activity.getString(R.string.welcome_location_permission_description))
        .assertIsDisplayed()
    composeTestRule.waitForIdle()
    try {
      composeTestRule.onNodeWithTag(WelcomeTestTags.LocationPermissionButton).assertIsDisplayed()
      composeTestRule.onNodeWithTag(WelcomeTestTags.LocationPermissionButton).performClick()
      allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
    } catch (e: AssertionError) {
      // Permission button not visible, already have permission
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).assertIsDisplayed()
    // Background permission button should not exist on SDK <= 28
    try {
      composeTestRule
          .onNodeWithTag(WelcomeTestTags.BackgroundLocationPermissionButton)
          .assertIsDisplayed()
      throw AssertionError(
          "Background location permission button should not be displayed on SDK <= 28")
    } catch (e: AssertionError) {
      if (e.message?.contains("should not be displayed") == true) {
        throw e
      }
      // Expected - button not found
    }
  }

  @SdkSuppress(minSdkVersion = 34)
  @Test
  fun welcome_activity_displays_correct_fragments_with_notification_permissions() {
    // Intro fragment
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.welcome_heading))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()

    // Connection setup fragment
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.welcome_connection_setup_title))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(
            composeTestRule.activity.getString(R.string.welcome_connection_setup_description))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()

    // Location permissions fragment
    composeTestRule
        .onNodeWithText(
            composeTestRule.activity.getString(R.string.welcome_location_permission_description))
        .assertIsDisplayed()
    composeTestRule.waitForIdle()
    try {
      composeTestRule.onNodeWithTag(WelcomeTestTags.LocationPermissionButton).assertIsDisplayed()
      composeTestRule.onNodeWithTag(WelcomeTestTags.LocationPermissionButton).performClick()
      allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
    } catch (e: AssertionError) {
      // Permission button not visible, already have permission
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(WelcomeTestTags.BackgroundLocationPermissionButton)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()

    // Notification permissions fragment
    composeTestRule
        .onNodeWithText(
            composeTestRule.activity.getString(
                R.string.welcome_notification_permission_description))
        .assertIsDisplayed()
    composeTestRule.waitForIdle()
    try {
      composeTestRule
          .onNodeWithTag(WelcomeTestTags.NotificationPermissionButton)
          .assertIsDisplayed()
      composeTestRule.onNodeWithTag(WelcomeTestTags.NotificationPermissionButton).performClick()
      allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)
    } catch (e: AssertionError) {
      // Permission button not visible, already have permission
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()

    // Done fragment
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.done_heading))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.enjoy_description))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(
            composeTestRule.activity.getString(
                R.string.welcome_finish_open_preferences_button_label))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.DoneButton).assertIsDisplayed()
  }

  @Test
  @SdkSuppress(minSdkVersion = 24, maxSdkVersion = 33)
  fun welcome_activity_displays_correct_fragments() {
    // Intro fragment
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.welcome_heading))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()

    // Connection setup fragment
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.welcome_connection_setup_title))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(
            composeTestRule.activity.getString(R.string.welcome_connection_setup_description))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()

    // Location permissions fragment
    composeTestRule
        .onNodeWithText(
            composeTestRule.activity.getString(R.string.welcome_location_permission_description))
        .assertIsDisplayed()
    composeTestRule.waitForIdle()
    try {
      composeTestRule.onNodeWithTag(WelcomeTestTags.LocationPermissionButton).assertIsDisplayed()
      composeTestRule.onNodeWithTag(WelcomeTestTags.LocationPermissionButton).performClick()
      allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
    } catch (e: AssertionError) {
      // Permission button not visible, already have permission
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()

    // Done fragment
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.done_heading))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.enjoy_description))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(
            composeTestRule.activity.getString(
                R.string.welcome_finish_open_preferences_button_label))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(WelcomeTestTags.DoneButton).assertIsDisplayed()
  }

  @Test
  fun welcome_activity_can_be_swiped_back_to_start() {
    composeTestRule.onNodeWithTag(WelcomeTestTags.NextButton).performClick()
    composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.welcome_heading))
        .assertIsDisplayed()
  }
}
