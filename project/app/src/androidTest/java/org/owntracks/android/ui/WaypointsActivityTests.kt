package org.owntracks.android.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertDisabled
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.getText
import org.owntracks.android.testutils.grantNotificationAndForegroundPermissions
import org.owntracks.android.ui.waypoints.WaypointsActivity

@OptIn(ExperimentalUnsignedTypes::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class WaypointsActivityTests :
    TestWithAnActivity<WaypointsActivity>(WaypointsActivity::class.java),
    TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl() {
  @Before
  fun grantPermissions() {
    grantNotificationAndForegroundPermissions()
  }

  @Test
  fun initialRegionsActivityIsEmpty() {
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
    clickOnAndWait(R.id.add)
    writeTo(R.id.description, waypointName)
    writeTo(R.id.latitude, latitude.toString())
    writeTo(R.id.longitude, longitude.toString())
    writeTo(R.id.radius, radius.toString())

    clickOnAndWait(R.id.save)

    assertDisplayed(waypointName)

    clickOnAndWait(waypointName)

    assertContains(R.id.description, waypointName)
    assertContains(R.id.latitude, expectedLatitude.toString())
    assertContains(R.id.longitude, expectedLongitude.toString())
    assertContains(R.id.radius, radius.toString())
  }

  @Test
  fun whenAddingAWaypointThenTheWaypointIsShown() {
    val waypointName = "test waypoint"
    val latitude = 51.123
    val longitude = 0.456
    val radius = 159
    addWaypoint(waypointName, latitude, longitude, radius)
  }

  @Test
  fun whenAddingAWaypointWithNegativeLongitudeThenTheWaypointIsShown() {
    val waypointName = "test waypoint"
    val latitude = 51.123
    val longitude = -0.456
    val radius = 159
    addWaypoint(waypointName, latitude, longitude, radius)
  }

  @Test
  fun whenAddingAWaypointWithNegativeLatitudeThenTheWaypointIsShown() {
    val waypointName = "test waypoint"
    val latitude = -51.0
    val longitude = 0.456
    val radius = 159
    addWaypoint(waypointName, latitude, longitude, radius)
  }

  @Test
  fun whenAddingAWaypointWithOutOfRangeLatitudeThenTheLatitudeIsRanged() {
    val waypointName = "test waypoint"
    val latitude = -1234.0
    val expectedLatitude = -26.0
    val longitude = 0.456
    val radius = 159
    addWaypoint(waypointName, latitude, longitude, radius, expectedLatitude)
  }

  @Test
  fun whenAddingAWaypointWithOutOfRangeLongitudeThenTheLatitudeIsRanged() {
    val waypointName = "test waypoint"
    val latitude = 25.123
    val longitude = -974.0
    val expectedLongitude = 106.0
    val radius = 159
    addWaypoint(waypointName, latitude, longitude, radius, expectedLongitude = expectedLongitude)
  }

  @Test
  fun whenAddingAWaypointWithInvalidLatitudeThenAnErrorIsShown() {
    clickOnAndWait(R.id.add)
    writeTo(R.id.description, "test waypoint")
    writeTo(R.id.latitude, "0-3")
    writeTo(R.id.longitude, "0.123")
    writeTo(R.id.radius, "20")
    assertDisabled(R.id.save)
    onView(withId(R.id.latitude))
        .check(matches(hasErrorText(app.getString(R.string.invalidLatitudeError))))
  }

  @Test
  fun whenAddingAWaypointWithInvalidLongitudeThenAnErrorIsShown() {
    clickOnAndWait(R.id.add)
    writeTo(R.id.description, "test waypoint")
    writeTo(R.id.latitude, "0")
    writeTo(R.id.longitude, "0.123-3")
    writeTo(R.id.radius, "20")
    assertDisabled(R.id.save)
    onView(withId(R.id.longitude))
        .check(matches(hasErrorText(app.getString(R.string.invalidLongitudeError))))
  }

  @Test
  fun whenAddingAWaypointAndThenDeletingItThenTheWaypointIsNotShown() {
    val waypointName = "test waypoint to be deleted"
    val latitude = 51.123
    val longitude = 0.456
    val radius = 159
    clickOnAndWait(R.id.add)
    writeTo(R.id.description, waypointName)
    writeTo(R.id.latitude, latitude.toString())
    writeTo(R.id.longitude, longitude.toString())
    writeTo(R.id.radius, radius.toString())

    clickOnAndWait(R.id.save)

    assertDisplayed(waypointName)

    clickOnAndWait(waypointName)

    clickOnAndWait(R.id.delete)
    clickOnAndWait(R.string.deleteWaypointConfirmationButtonLabel)

    assertNotDisplayed(waypointName)
  }

  @Test
  fun editorExportedConfigContainsCorrectWaypoints() {
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
