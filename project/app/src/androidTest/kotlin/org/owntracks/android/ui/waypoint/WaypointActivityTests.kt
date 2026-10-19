package org.owntracks.android.ui.waypoint

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity

class WaypointActivityTests : TestWithAnActivity<WaypointActivity>(true) {
  @Test
  fun saveButtonIsDisabledWhileCoordinateIsInvalidAndRecoversWhenItIsFixed() {
    writeTo(R.id.description, "test waypoint")
    writeTo(R.id.longitude, "1.234")
    writeTo(R.id.radius, "100")
    writeTo(R.id.latitude, "1.2.3")
    onView(withId(R.id.save)).check(matches(not(isEnabled())))
    writeTo(R.id.latitude, "51.5")
    onView(withId(R.id.save)).check(matches(isEnabled()))
  }
}
