package org.owntracks.android.e2e

import android.Manifest
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import com.adevinta.android.barista.interaction.PermissionGranter.allowPermissionsIfNeeded
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import java.util.concurrent.TimeUnit
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.testutils.GPSMockDeviceLocation
import org.owntracks.android.testutils.MockDeviceLocation
import org.owntracks.android.testutils.SmokeTest
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnHTTPServer
import org.owntracks.android.testutils.TestWithAnHTTPServerImpl
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.testutils.with
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.map.MapActivity

@LargeTest
@SmokeTest
@RunWith(AndroidJUnit4::class)
class LocationAccuracyTest :
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

    class LoggingMockJSONResponseDispatcher(private val responses: Map<String, String>) :
        Dispatcher() {
        private val requestBodies = mutableListOf<RecordedRequest>()
        val requestsReceived
            get() = requestBodies.size

        override fun dispatch(request: RecordedRequest): MockResponse {
            val errorResponse = MockResponse().setResponseCode(404)
            requestBodies.add(request)
            return responses[request.path]?.let {
                MockResponse().setResponseCode(200)
                    .setHeader("Content-type", "application/json")
                    .setBody(it)
            } ?: errorResponse
        }
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun testReportingLocationOutsideLocationAccuracyThreshold() {
        val dispatcher = LoggingMockJSONResponseDispatcher(mapOf("/" to "{}"))
        startServer(dispatcher)
        setNotFirstStartPreferences()

        PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
            .edit()
            .putInt(Preferences::ignoreInaccurateLocations.name, 50)
            .apply()

        launchActivity()

        allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
        initializeMockLocationProvider(baristaRule.activityTestRule.activity.applicationContext)

        configureHTTPConnectionToLocal()

        baristaRule.activityTestRule.activity.locationIdlingResource.with {
            setMockLocation(52.0, 0.0, 100f)
            clickOnAndWait(R.id.menu_report)
        }
        baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.with(TimeUnit.MINUTES.toSeconds(2)) {
            openDrawer()
            clickOnAndWait(R.string.title_activity_status)
        }
        sleep(1000) // The status needs time to react and appear
        assertContains(R.id.connectedStatus, R.string.IDLE)
        assertNotDisplayed(R.id.connectedStatusMessage)
        assertEquals(0, dispatcher.requestsReceived)
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun testReportingLocationInsideLocationAccuracyThreshold() {
        val dispatcher = LoggingMockJSONResponseDispatcher(mapOf("/" to "{}"))
        startServer(dispatcher)
        setNotFirstStartPreferences()

        PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
            .edit()
            .putInt(Preferences::ignoreInaccurateLocations.name, 50)
            .apply()

        launchActivity()

        allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
        initializeMockLocationProvider(baristaRule.activityTestRule.activity.applicationContext)

        configureHTTPConnectionToLocal()

        baristaRule.activityTestRule.activity.locationIdlingResource.with {
            setMockLocation(52.0, 0.0, 25f)
            clickOnAndWait(R.id.menu_report)
        }
        baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.with(TimeUnit.MINUTES.toSeconds(2)) {
            openDrawer()
            clickOnAndWait(R.string.title_activity_status)
        }
        sleep(1000) // The status needs time to react and appear
        assertContains(R.id.connectedStatus, R.string.IDLE)
        assertContains(R.id.connectedStatusMessage, "1 msg")
        assertNotEquals(0, dispatcher.requestsReceived)
    }
}
