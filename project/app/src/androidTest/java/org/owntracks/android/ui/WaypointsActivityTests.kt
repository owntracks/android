package org.owntracks.android.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.ui.waypoints.WaypointsActivity

@OptIn(ExperimentalUnsignedTypes::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class WaypointsActivityTests :
    TestWithAnActivity<WaypointsActivity>(WaypointsActivity::class.java),
    TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl() {
    @Test
    fun initialRegionsActivityIsEmpty() {
        assertDisplayed(R.string.waypointListPlaceholder)
        assertDisplayed(R.id.add)
    }

    @Test
    fun whenAddingAWaypointThenTheWaypointIsShown() {
        val waypointName = "test waypoint"
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

        assertContains(R.id.description, waypointName)
        assertContains(R.id.latitude, latitude.toString())
        assertContains(R.id.longitude, longitude.toString())
        assertContains(R.id.radius, radius.toString())
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
        clickOnAndWait(R.string.deleteWaypointTitle)

        assertNotDisplayed(waypointName)
    }
}
