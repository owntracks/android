package org.owntracks.android.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.MediumTest
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertDisabled
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.getText
import org.owntracks.android.testutils.grantNotificationAndForegroundPermissions
import org.owntracks.android.ui.waypoints.WaypointsActivity

@OptIn(ExperimentalUnsignedTypes::class)
@MediumTest
@HiltAndroidTest
class WaypointsActivityTests :
    TestWithAnActivity<WaypointsActivity>(), TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl() {

  @Before
  fun grantPermissions() {
    grantNotificationAndForegroundPermissions()
  }

  @Test
  fun initial_regions_activity_is_empty() {
    assertDisplayed(R.string.waypointListPlaceholder)
    assertDisplayed(R.id.add)
  }

  private fun addWaypoint(
      waypointName: String,
      latitude: Double,
      longitude: Double,
      radius: Int,
      expectedLatitude: Double = latitude,
      expectedLongitude: Double = longitude
  ) {
    clickOn(R.id.add)
    writeTo(R.id.description, waypointName)
    writeTo(R.id.latitude, latitude.toString())
    writeTo(R.id.longitude, longitude.toString())
    writeTo(R.id.radius, radius.toString())

    clickOn(R.id.save)

    assertDisplayed(waypointName)

    clickOn(waypointName)

    assertContains(R.id.description, waypointName)
    assertContains(R.id.latitude, expectedLatitude.toString())
    assertContains(R.id.longitude, expectedLongitude.toString())
    assertContains(R.id.radius, radius.toString())
  }

  @Test
  fun when_adding_a_waypoint_then_the_waypoint_is_shown() {
    val waypointName = "test waypoint"
    val latitude = 51.123
    val longitude = 0.456
    val radius = 159
    addWaypoint(waypointName, latitude, longitude, radius)
  }

  @Test
  fun when_adding_a_waypoint_with_negative_longitude_then_the_waypoint_is_shown() {
    val waypointName = "test waypoint"
    val latitude = 51.123
    val longitude = -0.456
    val radius = 159
    addWaypoint(waypointName, latitude, longitude, radius)
  }

  @Test
  fun when_adding_a_waypoint_with_negative_latitude_then_the_waypoint_is_shown() {
    val waypointName = "test waypoint"
    val latitude = -51.0
    val longitude = 0.456
    val radius = 159
    addWaypoint(waypointName, latitude, longitude, radius)
  }

  @Test
  fun when_adding_a_waypoint_with_out_of_range_latitude_then_the_latitude_is_ranged() {
    val waypointName = "test waypoint"
    val latitude = -1234.0
    val expectedLatitude = -26.0
    val longitude = 0.456
    val radius = 159
    addWaypoint(waypointName, latitude, longitude, radius, expectedLatitude)
  }

  @Test
  fun when_adding_a_waypoint_with_out_of_range_longitude_then_the_latitude_is_ranged() {
    val waypointName = "test waypoint"
    val latitude = 25.123
    val longitude = -974.0
    val expectedLongitude = 106.0
    val radius = 159
    addWaypoint(waypointName, latitude, longitude, radius, expectedLongitude = expectedLongitude)
  }

  @Test
  fun when_adding_a_waypoint_with_invalid_latitude_then_an_error_is_shown() {
    clickOn(R.id.add)
    writeTo(R.id.description, "test waypoint")
    writeTo(R.id.latitude, "0-3")
    writeTo(R.id.longitude, "0.123")
    writeTo(R.id.radius, "20")
    assertDisabled(R.id.save)
    onView(withId(R.id.latitude))
        .check(matches(hasErrorText(app.getString(R.string.invalidLatitudeError))))
  }

  @Test
  fun when_adding_a_waypoint_with_invalid_longitude_then_an_error_is_shown() {
    clickOn(R.id.add)
    writeTo(R.id.description, "test waypoint")
    writeTo(R.id.latitude, "0")
    writeTo(R.id.longitude, "0.123-3")
    writeTo(R.id.radius, "20")
    assertDisabled(R.id.save)
    onView(withId(R.id.longitude))
        .check(matches(hasErrorText(app.getString(R.string.invalidLongitudeError))))
  }

  @Test
  fun when_adding_a_waypoint_and_then_deleting_it_then_the_waypoint_is_not_shown() {
    val waypointName = "test waypoint to be deleted"
    val latitude = 51.123
    val longitude = 0.456
    val radius = 159
    clickOn(R.id.add)
    writeTo(R.id.description, waypointName)
    writeTo(R.id.latitude, latitude.toString())
    writeTo(R.id.longitude, longitude.toString())
    writeTo(R.id.radius, radius.toString())

    clickOn(R.id.save)

    assertDisplayed(waypointName)

    clickOn(waypointName)

    clickOn(R.id.delete)
    clickOn(R.string.deleteWaypointConfirmationButtonLabel)

    assertNotDisplayed(waypointName)
  }

  @Test
  fun editor_exported_config_contains_correct_waypoints() {
    val waypointName = "test waypoint to be deleted"
    val latitude = 51.123
    val longitude = 0.456
    val radius = 159

    clickOn(R.id.add)
    writeTo(R.id.description, waypointName)
    writeTo(R.id.latitude, latitude.toString())
    writeTo(R.id.longitude, longitude.toString())
    writeTo(R.id.radius, radius.toString())
    clickOn(R.id.save)
    openDrawer()
    clickOn(R.string.title_activity_preferences)
    clickOn(R.string.configurationManagement)
    val effectiveConfiguration = getText(onView(withId(R.id.effectiveConfiguration)))
    val json = ObjectMapper().readTree(effectiveConfiguration)
    assertTrue(json.isObject)
    assertTrue(json.has("waypoints"))
    assertEquals(1, json["waypoints"].size())
    assertEquals(waypointName, json["waypoints"][0]["desc"].asText())
    assertEquals(latitude, json["waypoints"][0]["lat"].asDouble(), 0.0001)
    assertEquals(longitude, json["waypoints"][0]["lon"].asDouble(), 0.0001)
    assertEquals(radius, json["waypoints"][0]["rad"].asInt())
  }
}
