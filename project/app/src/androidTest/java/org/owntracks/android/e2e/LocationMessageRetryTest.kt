package org.owntracks.android.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import kotlin.time.Duration.Companion.minutes
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.GPSMockDeviceLocation
import org.owntracks.android.testutils.MockDeviceLocation
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnHTTPServer
import org.owntracks.android.testutils.TestWithAnHTTPServerImpl
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.reportLocationFromMap
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.testutils.use
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.map.MapActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class LocationMessageRetryTest :
    TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    TestWithAnHTTPServer by TestWithAnHTTPServerImpl(),
    MockDeviceLocation by GPSMockDeviceLocation() {

    //language=JSON
    private val locationResponse = """
        {"_type":"location","acc":20,"al":0,"batt":100,"bs":0,"conn":"w","created_at":1610748273,"lat":51.2,"lon":-4,"tid":"aa","tst":1610799026,"vac":40,"vel":7}
    """.trimIndent()

    @Test
    fun testReportingLocationSucceedsAfterSomeFailures() {
        startServer(FlakyWebServerDispatcher(locationResponse))
        setNotFirstStartPreferences()
        launchActivity()

        grantMapActivityPermissions()
        initializeMockLocationProvider(app)

        configureHTTPConnectionToLocal()

        reportLocationFromMap(app.mockLocationIdlingResource) {
            setMockLocation(51.0, 0.0)
        }

        baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use(2.minutes) {
            openDrawer()
            clickOnAndWait(R.string.title_activity_status)
        }
        sleep(1000) // The status needs time to react and appear
        assertContains(R.id.connectedStatusMessage, "Response 200")
    }

    class FlakyWebServerDispatcher(private val responseBody: String) : Dispatcher() {
        private var requestCounter = 0
        override fun dispatch(request: RecordedRequest): MockResponse {
            val errorResponse = MockResponse().setResponseCode(404)
            return if (request.path == "/") {
                requestCounter += 1
                if (requestCounter >= 3) {
                    MockResponse().setResponseCode(200)
                        .setHeader("Content-type", "application/json")
                        .setBody(responseBody)
                } else {
                    errorResponse
                }
            } else {
                errorResponse
            }
        }
    }
}
