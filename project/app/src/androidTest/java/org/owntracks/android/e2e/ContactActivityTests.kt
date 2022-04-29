package org.owntracks.android.e2e

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickBack
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import com.adevinta.android.barista.interaction.PermissionGranter
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.*
import org.owntracks.android.ui.clickBackAndWait
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.clickOnDrawerAndWait
import org.owntracks.android.ui.map.MapActivity


@LargeTest
@RunWith(AndroidJUnit4::class)
class ContactActivityTests : TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
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

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
        initializeMockLocationProvider(baristaRule.activityTestRule.activity.applicationContext)

        openDrawer()
        clickOnAndWait(R.string.title_activity_preferences)
        clickOnAndWait(R.string.preferencesServer)
        clickOnAndWait(R.string.mode_heading)
        clickOnAndWait(R.string.mode_http_private_label)
        clickDialogPositiveButton()
        clickOnAndWait(R.string.preferencesHost)
        writeTo(R.id.url, "http://localhost:${webserverPort}/")
        clickDialogPositiveButton()
        clickBack()

        openDrawer()
        clickOnDrawerAndWait(R.string.title_activity_map)

        baristaRule.activityTestRule.activity.locationIdlingResource.with {
            setMockLocation(51.0, 0.0)
            clickOnAndWait(R.id.menu_report)
        }

        baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.with {
            openDrawer()
        }
        clickOnAndWait(R.string.title_activity_contacts)
        sleep(3_000)
        assertRecyclerViewItemCount(R.id.recycler_view, 1)

        clickOnAndWait("aa")
        assertDisplayed(R.id.bottomSheetLayout)
        assertDisplayed(R.id.contactPeek)
        assertContains(R.id.name, "aa")

        clickBackAndWait()

        assertNotDisplayed(R.id.bottomSheetLayout)
        assertNotDisplayed(R.id.contactPeek)
    }
}