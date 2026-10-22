package org.owntracks.android.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.filters.MediumTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.time.Duration
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.location.profiles.ChargingSource
import org.owntracks.android.location.profiles.Condition
import org.owntracks.android.location.profiles.ContextProfile
import org.owntracks.android.location.profiles.ContextProfileManager
import org.owntracks.android.location.profiles.LocatorOverrides
import org.owntracks.android.location.profiles.decodeContextProfiles
import org.owntracks.android.location.profiles.encodeContextProfiles
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.clickOnPreference
import org.owntracks.android.testutils.di.FakeDeviceContextProvider
import org.owntracks.android.testutils.scrollToPreferenceWithText
import org.owntracks.android.testutils.writeToPreference
import org.owntracks.android.ui.preferences.PreferencesActivity

@MediumTest
@HiltAndroidTest
class ContextProfilesPreferencesTests : TestWithAnActivity<PreferencesActivity>() {
  @Inject lateinit var preferences: Preferences

  @Inject lateinit var device: FakeDeviceContextProvider

  @Inject lateinit var contextProfileManager: ContextProfileManager

  private fun profile(id: String, name: String) =
      ContextProfile(id, name, listOf(Condition.Charging), LocatorOverrides())

  private val profiles
    get() = decodeContextProfiles(preferences.contextProfiles)

  @Test
  fun context_profiles_are_in_the_preferences() {
    scrollToPreferenceWithText(R.string.preferencesContextProfiles)
    assertDisplayed(R.string.preferencesContextProfiles)
  }

  @Test
  fun a_profile_can_be_added_and_configured() {
    clickOnPreference(R.string.preferencesContextProfiles)
    clickOnPreference(R.string.preferencesContextProfilesEnabled)
    clickOnPreference(R.string.preferencesContextProfilesAdd)

    writeToPreference(R.string.preferencesContextProfileName, "Car")
    clickOnPreference(R.string.preferencesMonitoring)
    clickOn(R.string.monitoringModeDialogMoveTitle)
    clickOnPreference(R.string.preferencesContextProfileAddCondition)
    clickOn(R.string.contextProfileConditionTypeCharging)

    assertTrue(preferences.contextProfilesEnabled)
    profiles.single().run {
      assertEquals("Car", name)
      assertEquals(MonitoringMode.Move, overrides.monitoring)
      assertEquals(listOf(Condition.Charging), conditions)
    }
  }

  @Test
  fun a_profile_can_be_deleted() {
    preferences.contextProfiles = encodeContextProfiles(listOf(profile("a", "Car")))
    clickOnPreference(R.string.preferencesContextProfiles)
    clickOn("Car")

    scrollToPreferenceWithText(R.string.preferencesContextProfileDelete)
    clickOnPreference(R.string.preferencesContextProfileDelete)
    clickDialogPositiveButton()

    assertEquals(emptyList<ContextProfile>(), profiles)
  }

  @Test
  fun profiles_can_be_reordered() {
    preferences.contextProfiles =
        encodeContextProfiles(listOf(profile("a", "First"), profile("b", "Second")))
    clickOnPreference(R.string.preferencesContextProfiles)

    // Only the first profile can be moved down, the last one's button is hidden
    onView(allOf(withContentDescription(R.string.preferencesContextProfileMoveDown), isDisplayed()))
        .perform(click())

    assertEquals(listOf("b", "a"), profiles.map { it.id })
  }

  @Test
  fun the_profile_that_applies_is_shown() {
    contextProfileManager.deviceContextSettleTime = Duration.ZERO
    preferences.contextProfiles = encodeContextProfiles(listOf(profile("a", "Plugged in")))
    preferences.contextProfilesEnabled = true
    device.setCharging(ChargingSource.Cable)
    clickOnPreference(R.string.preferencesContextProfiles)

    assertDisplayed(R.string.preferencesContextProfilesStatus)
    // The status shows its name, and its row in the list says it's active
    assertDisplayed(
        app.getString(
            R.string.contextProfileSummaryActive,
            app.getString(R.string.contextProfileConditionCharging),
        )
    )
  }
}
