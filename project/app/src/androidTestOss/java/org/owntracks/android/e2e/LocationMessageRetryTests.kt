package org.owntracks.android.e2e

import android.Manifest
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.PermissionGranter
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnHTTPServer
import org.owntracks.android.testutils.TestWithAnHTTPServerImpl
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.map.MapActivity
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class LocationMessageRetryTests : TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    TestWithAnHTTPServer by TestWithAnHTTPServerImpl() {

    private val locationResponse = """
        {"_type":"location","acc":20,"al":0,"batt":100,"bs":0,"conn":"w","created_at":1610748273,"lat":51.2,"lon":-4,"tid":"aa","tst":1610799026,"vac":40,"vel":7}
    """.trimIndent()

    @Before
    fun setIdlingTimeout() {
        // We're going to fail to respond in this test for a bit, so need to slacken the idle timeout
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)
    }

    @Before
    fun startMockWebserver() {
        startServer(MockWebserverLocationDispatcher(locationResponse))
    }

    @After
    fun stopMockWebserver() {
        stopServer()
    }

    @After
    fun unregisterIdlingResource() {
        try {
            IdlingRegistry.getInstance()
                .unregister(baristaRule.activityTestRule.activity.locationIdlingResource)
        } catch (_: NullPointerException) {
            // Happens when the vm is already gone from the MapActivity
        }
        try {
            IdlingRegistry.getInstance()
                .unregister(baristaRule.activityTestRule.activity.outgoingQueueIdlingResource)
        } catch (_: NullPointerException) {
        }
    }

    @Test
    @AllowFlaky
    fun testReportingLocationSucceedsAfterSomeFailures() {
        setNotFirstStartPreferences()
        baristaRule.launchActivity()

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)

        configureHTTPConnectionToLocal()

        openDrawer()
        clickOnAndWait(R.string.title_activity_map)

        val locationIdlingResource = baristaRule.activityTestRule.activity.locationIdlingResource
        IdlingPolicies.setIdlingResourceTimeout(30, TimeUnit.SECONDS)
        IdlingRegistry.getInstance().register(locationIdlingResource)
        clickOnAndWait(R.id.menu_report)

        val outgoingQueueIdlingResource =
            baristaRule.activityTestRule.activity.outgoingQueueIdlingResource
        IdlingRegistry.getInstance().register(outgoingQueueIdlingResource)

        openDrawer()
        clickOnAndWait(R.string.title_activity_status)

        assertContains(R.id.connectedStatusMessage, "Response 200")
    }

    class MockWebserverLocationDispatcher(private val config: String) : Dispatcher() {
        private var requestCounter = 0
        override fun dispatch(request: RecordedRequest): MockResponse {
            val errorResponse = MockResponse().setResponseCode(404)
            return if (request.path == "/") {
                requestCounter += 1
                if (requestCounter >= 3) {
                    MockResponse().setResponseCode(200)
                        .setHeader("Content-type", "application/json").setBody(config)
                } else {
                    errorResponse
                }
            } else {
                errorResponse
            }
        }
    }
}