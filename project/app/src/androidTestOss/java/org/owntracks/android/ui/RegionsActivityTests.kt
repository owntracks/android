package org.owntracks.android.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.longClickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.rules.ScreenshotTakingOnTestEndRule
import org.owntracks.android.ui.regions.RegionsActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class RegionsActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(RegionsActivity::class.java)

    private val screenshotRule = ScreenshotTakingOnTestEndRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(baristaRule.activityTestRule)
        .around(screenshotRule)

    @Before
    fun setUp() {
        baristaRule.launchActivity()
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun initialRegionsActivityIsEmpty() {
        assertDisplayed(R.string.waypointListPlaceholder)
        assertDisplayed(R.id.add)
    }

    @Test
    @AllowFlaky(attempts = 1)
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
    @AllowFlaky(attempts = 1)
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

        assertNotExist(regionName)
    }

}