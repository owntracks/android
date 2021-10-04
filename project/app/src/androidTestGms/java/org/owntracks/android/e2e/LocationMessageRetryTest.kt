package org.owntracks.android.e2e

import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickBack
import com.schibsted.spain.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.schibsted.spain.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.schibsted.spain.barista.interaction.BaristaEditTextInteractions.writeTo
import com.schibsted.spain.barista.rule.BaristaRule
import com.schibsted.spain.barista.rule.flaky.AllowFlaky
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.owntracks.android.App
import org.owntracks.android.R
import org.owntracks.android.ScreenshotTakingOnTestEndRule
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.map.MapActivity
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class LocationMessageRetryTest {
    @get:Rule
    var baristaRule =
        BaristaRule.create(MapActivity::class.java) // We always start e2e at the main entrypoint

    private val screenshotRule = ScreenshotTakingOnTestEndRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(baristaRule.activityTestRule)
        .around(screenshotRule)

    private var mockWebServer = MockWebServer()

    @Before
    fun setIdlingTimeout() {
        // We're going to fail to respond in this test for a bit, so need to slacken the idle timeout
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)
    }

    @Before
    fun startMockWebserver() {
        try {
            mockWebServer.start()
        } catch (e: IllegalArgumentException) {
            // Already started
        }
        mockWebServer.dispatcher = MockWebserverLocationDispatcher(locationResponse)
    }

    @After
    fun stopMockWebserver() {
        mockWebServer.shutdown()
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

    private val locationResponse = """
        {"_type":"location","acc":20,"al":0,"batt":100,"bs":0,"conn":"w","created_at":1610748273,"lat":51.2,"lon":-4,"tid":"aa","tst":1610799026,"vac":40,"vel":7}
    """.trimIndent()

    @Test
    @AllowFlaky
    fun testReportingLocationSucceedsAfterSomeFailures() {
        baristaRule.launchActivity()

        val httpPort = mockWebServer.port
        doWelcomeProcess(baristaRule.activityTestRule.activity.application as App)

        openDrawer()
        clickOnAndWait(R.string.title_activity_preferences)
        clickOnAndWait(R.string.preferencesServer)
        clickOnAndWait(R.string.mode_heading)
        clickOnAndWait(R.string.mode_http_private_label)
        clickDialogPositiveButton()
        clickOnAndWait(R.string.preferencesHost)
        writeTo(R.id.url, "http://localhost:${httpPort}/")
        clickDialogPositiveButton()
        clickBack()

        openDrawer()
        clickOnAndWait(R.string.title_activity_map)

        val locationIdlingResource = baristaRule.activityTestRule.activity.locationIdlingResource
        IdlingRegistry.getInstance().register(locationIdlingResource)
        clickOnAndWait(R.id.menu_report)

        val networkIdlingResource =
            baristaRule.activityTestRule.activity.outgoingQueueIdlingResource
        IdlingRegistry.getInstance().register(networkIdlingResource)

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