package org.owntracks.android.e2e

import androidx.preference.PreferenceManager
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
    fun testReportingLocationOutsideLocationAccuracyThreshold() {
        val dispatcher = LoggingMockJSONResponseDispatcher(mapOf("/" to "{}"))
        val inaccurateMockLatitude = 52.0
        val inaccurateMockLongitude = 0.0
        val accurateMockLatitude = 52.1
        val accurateMockLongitude = 0.1
        startServer(dispatcher)
        setNotFirstStartPreferences()

        PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
            .edit()
            .putInt(Preferences::ignoreInaccurateLocations.name, 50)
            .apply()

        launchActivity()

        grantMapActivityPermissions()
        initializeMockLocationProvider(app)

        configureHTTPConnectionToLocal()

        reportLocationFromMap(baristaRule.activityTestRule.activity.locationIdlingResource) {
            setMockLocation(inaccurateMockLatitude, inaccurateMockLongitude, 100f)
        }

        reportLocationFromMap(baristaRule.activityTestRule.activity.locationIdlingResource) {
            setMockLocation(accurateMockLatitude, accurateMockLongitude, 3f)
        }

        dispatcher.requestBodies.map { it.body.readUtf8() }
            .run {
                assertTrue(
                    any {
                        it.isNotEmpty() && JSONObject(it).run {
                            getDouble("lat") == accurateMockLatitude && getDouble("lon") == accurateMockLongitude
                        }
                    }
                )
                assertFalse(
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
        startServer(dispatcher)
        setNotFirstStartPreferences()

        PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
            .edit()
            .putInt(Preferences::ignoreInaccurateLocations.name, 50)
            .apply()

        launchActivity()

        grantMapActivityPermissions()
        initializeMockLocationProvider(app)

        configureHTTPConnectionToLocal()
        reportLocationFromMap(baristaRule.activityTestRule.activity.locationIdlingResource) {
            setMockLocation(mockLatitude, mockLongitude, 25f)
        }
        assertTrue(
            dispatcher.requestBodies.any {
                JSONObject(it.body.readUtf8()).run {
                    getInt("acc") == 25 && getDouble("lat") == mockLatitude && getDouble("lon") == mockLongitude
                }
            }
        )
    }
}
