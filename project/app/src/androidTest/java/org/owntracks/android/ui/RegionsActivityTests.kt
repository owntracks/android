package org.owntracks.android.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.longClickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.ui.regions.RegionsActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class RegionsActivityTests : TestWithAnActivity<RegionsActivity>(RegionsActivity::class.java) {
    @Test
    fun initialRegionsActivityIsEmpty() {
        assertDisplayed(R.string.waypointListPlaceholder)
        assertDisplayed(R.id.add)
    }

    @Test
    fun whenAddingARegionThenTheRegionIsShown() {
        val regionName = "test region"
        val latitude = 51.123
        val longitude = 0.456
        val radius = 159
        clickOnAndWait(R.id.add)
        writeTo(R.id.description, regionName)
        writeTo(R.id.latitude, latitude.toString())
        writeTo(R.id.longitude, longitude.toString())
        writeTo(R.id.radius, radius.toString())

        clickOnAndWait(R.id.save)

        assertDisplayed(regionName)

        clickOnAndWait(regionName)

        assertContains(R.id.description, regionName)
        assertContains(R.id.latitude, latitude.toString())
        assertContains(R.id.longitude, longitude.toString())
        assertContains(R.id.radius, radius.toString())
    }

    @Test
    @Ignore("Switch from long click to delete to a specific action in the waypoint")
    fun whenAddingARegionAndThenDeletingItThenTheRegionIsNotShown() {
        val regionName = "test region to be deleted"
        val latitude = 51.123
        val longitude = 0.456
        val radius = 159
        clickOnAndWait(R.id.add)
        writeTo(R.id.description, regionName)
        writeTo(R.id.latitude, latitude.toString())
        writeTo(R.id.longitude, longitude.toString())
        writeTo(R.id.radius, radius.toString())

        clickOnAndWait(R.id.save)

        assertDisplayed(regionName)

        longClickOn(regionName)

        clickOnAndWait("Delete")

        assertNotDisplayed(regionName)
    }
}
