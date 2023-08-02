package org.owntracks.android.e2e

import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.testutils.*
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber

@LargeTest
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

    class LoggingMockJSONResponseDispatcher(private val responses: Map<String, String>) : Dispatcher() {
        val requestBodies = mutableListOf<RecordedRequest>()

        override fun dispatch(request: RecordedRequest): MockResponse {
            val errorResponse = MockResponse().setResponseCode(404)
            requestBodies.add(request)
            Timber.v("Received request $request")
            return responses[request.path]?.let {
                MockResponse().setResponseCode(200)
                    .setHeader("Content-type", "application/json")
                    .setBody(it)
            } ?: errorResponse
        }
    }

    @Test
    fun given_an_inaccurate_and_accurate_location_when_publishing_then_only_the_location_only_the_accurate_location_is_published() {
        val dispatcher = LoggingMockJSONResponseDispatcher(mapOf("/" to "{}"))
        val inaccurateMockLatitude = 52.0
        val inaccurateMockLongitude = 0.0
        val accurateMockLatitude = 22.1
        val accurateMockLongitude = 5.1

        PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
            .edit()
            .putInt(Preferences::ignoreInaccurateLocations.name, 50)
            .apply()

        setupForHttp(dispatcher)

        reportLocationFromMap(app.mockLocationIdlingResource) {
            setMockLocation(inaccurateMockLatitude, inaccurateMockLongitude, 100f)
        }

        app.mockLocationIdlingResource.setIdleState(false)

        reportLocationFromMap(app.mockLocationIdlingResource) {
            setMockLocation(accurateMockLatitude, accurateMockLongitude, 3f)
        }

        baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use {
            Espresso.onIdle()
        }

        dispatcher.requestBodies.map { it.body.readUtf8() }
            .run {
                Timber.d("HTTP request bodies: $this")
                assertTrue(
                    "Published location contains location under accuracy threshold (lat=$accurateMockLatitude, lon=$accurateMockLongitude)", // ktlint-disable max-line-length
                    any {
                        it.isNotEmpty() && JSONObject(it).run {
                            getDouble("lat") == accurateMockLatitude && getDouble("lon") == accurateMockLongitude
                        }
                    }
                )
                assertFalse(
                    "Published location doesn't contain location over accuracy threshold",
                    any {
                        it.isNotEmpty() && JSONObject(it).run {
                            getDouble("lat") == inaccurateMockLatitude && getDouble("lon") == inaccurateMockLongitude
                        }
                    }
                )
            }
    }

    @Test
    fun testReportingLocationInsideLocationAccuracyThreshold() {
        val dispatcher = LoggingMockJSONResponseDispatcher(mapOf("/" to "{}"))
        val mockLatitude = 51.0
        val mockLongitude = 0.0
        PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
            .edit()
            .putInt(Preferences::ignoreInaccurateLocations.name, 50)
            .apply()

        setupForHttp(dispatcher)

        reportLocationFromMap(app.mockLocationIdlingResource) {
            setMockLocation(mockLatitude, mockLongitude, 25f)
        }
        baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use {
            Espresso.onIdle()
        }
        val bodies = dispatcher.requestBodies.map { it.body.readUtf8() }
        assertTrue(
            bodies.any {
                JSONObject(it).run {
                    getInt("acc") == 25 && getDouble("lat") == mockLatitude && getDouble("lon") == mockLongitude
                }
            }
        )
    }
    private fun setupForHttp(dispatcher: LoggingMockJSONResponseDispatcher) {
        startServer(dispatcher)
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
        initializeMockLocationProvider(app)
        configureHTTPConnectionToLocal()
        waitUntilActivityVisible<MapActivity>()
    }
}
