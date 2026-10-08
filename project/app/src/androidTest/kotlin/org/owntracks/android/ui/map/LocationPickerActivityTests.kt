package org.owntracks.android.ui.map

import android.app.Activity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.MediumTest
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.location.LatLng
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.ui.map.picker.LocationPickerActivity
import org.owntracks.android.ui.map.picker.LocationPickerActivity.PickLocation

@MediumTest
@HiltAndroidTest
class LocationPickerActivityTests : TestWithAnActivity<LocationPickerActivity>(false) {
  private val pickLocation = PickLocation()

  private fun launchPicker(canClear: Boolean = false) =
      launchActivity(
          pickLocation.createIntent(app, PickLocation.Input(LatLng(51.0, 1.0), canClear))
      )

  private val activityResult
    get() = baristaRule.activityTestRule.activityResult

  private val picked
    get() = activityResult.run { pickLocation.parseResult(resultCode, resultData) }

  @Test
  fun saving_without_moving_the_map_picks_where_it_started() {
    launchPicker()
    clickOn(R.id.save)
    (picked as LocationPickerActivity.Result.Picked).latLng.run {
      assertEquals(51.0, latitude.value, 0.001)
      assertEquals(1.0, longitude.value, 0.001)
    }
  }

  @Test
  fun typed_coordinates_are_picked() {
    launchPicker()
    onView(withId(R.id.coordinates))
        .perform(click(), replaceText("40.5, -111.25"), closeSoftKeyboard())
    clickOn(R.id.save)
    (picked as LocationPickerActivity.Result.Picked).latLng.run {
      assertEquals(40.5, latitude.value, 0.001)
      assertEquals(-111.25, longitude.value, 0.001)
    }
  }

  @Test
  fun a_static_location_can_be_cleared() {
    launchPicker(canClear = true)
    clickOn(R.id.clear)
    assertEquals(LocationPickerActivity.Result.Cleared, picked)
  }

  @Test
  fun backing_out_picks_nothing() {
    launchPicker(canClear = true)
    pressBackUnconditionally()
    assertEquals(Activity.RESULT_CANCELED, activityResult.resultCode)
  }
}
