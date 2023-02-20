package org.owntracks.android.e2e

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import com.adevinta.android.barista.interaction.PermissionGranter
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.*
import org.owntracks.android.ui.clickBackAndWait
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.map.MapActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class ContactActivityTests :
    TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    TestWithAnHTTPServer by TestWithAnHTTPServerImpl(),
    MockDeviceLocation by GPSMockDeviceLocation() {
    @After
    fun stopMockWebserver() {
        stopServer()
    }

    @After
    fun removeMockLocationProvider() {
        unInitializeMockLocationProvider()
    }

    private val locationResponse = """
        {"_type":"location","acc":20,"al":0,"batt":100,"bs":0,"conn":"w","created_at":1610748273,"lat":51.2,"lon":-4,"tid":"aa","tst":1610799026,"vac":40,"vel":7}
    """.trimIndent()

    @Test
    fun testClickingOnContactLoadsContactOnMap() {
        startServer(mapOf("/" to locationResponse))
        setNotFirstStartPreferences()
        launchActivity()

        grantMapActivityPermissions()
        initializeMockLocationProvider(app)
        configureHTTPConnectionToLocal()

        reportLocationFromMap(baristaRule.activityTestRule.activity.locationIdlingResource) {
            setMockLocation(51.0, 0.0)
        }

        baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.with {
            openDrawer()
            clickOnAndWait(R.string.title_activity_contacts)
        }
        sleep(3_000)
        assertRecyclerViewItemCount(R.id.contactsRecyclerView, 1)

        clickOnAndWait("aa")
        assertDisplayed(R.id.bottomSheetLayout)
        assertDisplayed(R.id.contactPeek)
        assertContains(R.id.name, "aa")

        clickBackAndWait()

        assertNotDisplayed(R.id.bottomSheetLayout)
        assertNotDisplayed(R.id.contactPeek)
    }
}
