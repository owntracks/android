package org.owntracks.android.ui

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import java.io.File
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.writeFileToDevice
import org.owntracks.android.ui.preferences.load.LoadActivity

@MediumTest
@RunWith(AndroidJUnit4::class)
class LoadActivityTests : TestWithAnActivity<LoadActivity>(LoadActivity::class.java, false) {
    private var mockWebServer = MockWebServer()

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    //language=JSON
    private val expectedConfig = """
{
  "_type" : "configuration",
  "waypoints" : [ {
    "_type" : "waypoint",
    "desc" : "work",
    "lat" : 51.5,
    "lon" : -0.02,
    "rad" : 150,
    "tst" : 1505910709000
  }, {
    "_type" : "waypoint",
    "desc" : "home",
    "lat" : 53.6,
    "lon" : -1.5,
    "rad" : 100,
    "tst" : 1558351273
  } ],
  "auth" : true,
  "autostartOnBoot" : true,
  "cleanSession" : false,
  "clientId" : "emulator",
  "cmd" : true,
  "connectionTimeoutSeconds" : 34,
  "debugLog" : true,
  "deviceId" : "testdevice",
  "enableMapRotation" : false,
  "fusedRegionDetection" : true,
  "geocodeEnabled" : true,
  "host" : "testhost.example.com",
  "ignoreInaccurateLocations" : 150,
  "ignoreStaleLocations" : 0,
  "keepalive" : 900,
  "locatorDisplacement" : 5,
  "locatorInterval" : 60,
  "mode" : 0,
  "monitoring" : 1,
  "moveModeLocatorInterval" : 10,
  "mqttProtocolLevel" : 3,
  "notificationHigherPriority" : false,
  "notificationLocation" : true,
  "opencageApiKey" : "",
  "osmTileScaleFactor" : 3.352,
  "password" : "password",
  "ping" : 30,
  "port" : 1883,
  "pubExtendedData" : true,
  "pubQos" : 1,
  "pubRetain" : true,
  "pubTopicBase" : "owntracks/%u/%d",
  "remoteConfiguration" : true,
  "sub" : true,
  "subQos" : 2,
  "subTopic" : "owntracks/+/+",
  "tls" : false,
  "usePassword" : true,
  "username" : "username",
  "ws" : false
}
    """.trimIndent()

    //language=JSON
    private val servedConfig =
        """{
  "_type": "configuration",
  "waypoints": [
    {
      "_type": "waypoint",
      "desc": "work",
      "lat": 51.5,
      "lon": -0.02,
      "rad": 150,
      "tst": 1505910709000
    },
    {
      "_type": "waypoint",
      "desc": "home",
      "lat": 53.6,
      "lon": -1.5,
      "rad": 100,
      "tst": 1558351273
    }
  ],
  "auth": true,
  "autostartOnBoot": true,
  "connectionTimeoutSeconds": 34,
  "cleanSession": false,
  "clientId": "emulator",
  "cmd": true,
  "debugLog": true,
  "deviceId": "testdevice",
  "fusedRegionDetection": true,
  "geocodeEnabled": true,
  "host": "testhost.example.com",
  "ignoreInaccurateLocations": 150,
  "ignoreStaleLocations": 0,
  "keepalive": 900,
  "locatorDisplacement": 5,
  "locatorInterval": 60,
  "mode": 0,
  "monitoring": 1,
  "enableMapRotation": false,
  "osmTileScaleFactor": 3.352,
  "moveModeLocatorInterval": 10,
  "mqttProtocolLevel": 3,
  "notificationHigherPriority": false,
  "notificationLocation": true,
  "opencageApiKey": "",
  "password": "password",
  "ping": 30,
  "port": 1883,
  "pubExtendedData": true,
  "pubQos": 1,
  "pubRetain": true,
  "pubTopicBase": "owntracks/%u/%d",
  "remoteConfiguration": true,
  "sub": true,
  "subQos": 2,
  "subTopic": "owntracks/+/+",
  "tls": false,
  "usePassword": true,
  "username": "username",
  "ws": false
}"""

    @Test
    fun loadActivityCanLoadConfigFromOwntracksInlineConfigURL() {
        launchActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "owntracks:///config?inline=eyJfdHlwZSI6ImNvbmZpZ3VyYXRpb24iLCJ3YXlwb2ludHMiOlt7Il90eXBlIjoid2F5cG9pbnQiLCJkZXNjIjoid29yayIsImxhdCI6NTEuNSwibG9uIjotMC4wMiwicmFkIjoxNTAsInRzdCI6MTUwNTkxMDcwOTAwMH0seyJfdHlwZSI6IndheXBvaW50IiwiZGVzYyI6ImhvbWUiLCJsYXQiOjUzLjYsImxvbiI6LTEuNSwicmFkIjoxMDAsInRzdCI6MTU1ODM1MTI3M31dLCJhdXRoIjp0cnVlLCJhdXRvc3RhcnRPbkJvb3QiOnRydWUsImNvbm5lY3Rpb25UaW1lb3V0U2Vjb25kcyI6MzQsImNsZWFuU2Vzc2lvbiI6ZmFsc2UsImNsaWVudElkIjoiZW11bGF0b3IiLCJjbWQiOnRydWUsImRlYnVnTG9nIjp0cnVlLCJkZXZpY2VJZCI6InRlc3RkZXZpY2UiLCJmdXNlZFJlZ2lvbkRldGVjdGlvbiI6dHJ1ZSwiZ2VvY29kZUVuYWJsZWQiOnRydWUsImhvc3QiOiJ0ZXN0aG9zdC5leGFtcGxlLmNvbSIsImlnbm9yZUluYWNjdXJhdGVMb2NhdGlvbnMiOjE1MCwiaWdub3JlU3RhbGVMb2NhdGlvbnMiOjAsImtlZXBhbGl2ZSI6OTAwLCJsb2NhdG9yRGlzcGxhY2VtZW50Ijo1LCJsb2NhdG9ySW50ZXJ2YWwiOjYwLCJtb2RlIjowLCJtb25pdG9yaW5nIjoxLCJlbmFibGVNYXBSb3RhdGlvbiI6ZmFsc2UsIm9zbVRpbGVTY2FsZUZhY3RvciI6My4zNTIsIm1vdmVNb2RlTG9jYXRvckludGVydmFsIjoxMCwibXF0dFByb3RvY29sTGV2ZWwiOjMsIm5vdGlmaWNhdGlvbkhpZ2hlclByaW9yaXR5IjpmYWxzZSwibm90aWZpY2F0aW9uTG9jYXRpb24iOnRydWUsIm9wZW5jYWdlQXBpS2V5IjoiIiwicGFzc3dvcmQiOiJwYXNzd29yZCIsInBpbmciOjMwLCJwb3J0IjoxODgzLCJwdWJFeHRlbmRlZERhdGEiOnRydWUsInB1YlFvcyI6MSwicHViUmV0YWluIjp0cnVlLCJwdWJUb3BpY0Jhc2UiOiJvd250cmFja3MvJXUvJWQiLCJyZW1vdGVDb25maWd1cmF0aW9uIjp0cnVlLCJzdWIiOnRydWUsInN1YlFvcyI6Miwic3ViVG9waWMiOiJvd250cmFja3MvKy8rIiwidGxzIjpmYWxzZSwidXNlUGFzc3dvcmQiOnRydWUsInVzZXJuYW1lIjoidXNlcm5hbWUiLCJ3cyI6ZmFsc2V9"
                )
            )
        )
        assertContains(R.id.effectiveConfiguration, expectedConfig)
        assertDisplayed(R.id.save)
        assertDisplayed(R.id.close)
    }

    @Test
    fun loadActivityShowsErrorWhenLoadingFromInlineConfigURLContainingInvalidJSON() {
        launchActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("owntracks:///config?inline=e30k")
            )
        )
        assertContains(
            R.id.importError,
            app.getString(R.string.errorPreferencesImportFailed, "Message is not a valid configuration message")
        )
        assertNotExist(R.id.save)
        assertDisplayed(R.id.close)
    }

    @Test
    fun loadActivityShowsErrorWhenLoadingFromInlineConfigURLContaninigInvalidBase64() {
        launchActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("owntracks:///config?inline=aaaaaaaaaaaaaaaaaaaaaaaaa")
            )
        )
        assertContains(
            R.id.importError,
            app.getString(R.string.errorPreferencesImportFailed, "")
        )
        assertNotExist(R.id.save)
        assertDisplayed(R.id.close)
    }

    @Test
    fun loadActivityCanLoadConfigFromOwntracksRemoteURL() {
        mockWebServer.start(8080)
        mockWebServer.dispatcher = MockWebserverConfigDispatcher(servedConfig)

        launchActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("owntracks:///config?url=http%3A%2F%2Flocalhost%3A8080%2Fmyconfig.otrc")
            )
        )
        sleep(1000)
        assertContains(R.id.effectiveConfiguration, expectedConfig)
        assertDisplayed(R.id.save)
        assertDisplayed(R.id.close)
    }

    @Test
    fun loadActivityShowsErrorTryingToLoadNotFoundRemoteUrl() {
        mockWebServer.start(8080)
        mockWebServer.dispatcher = MockWebserverConfigDispatcher(servedConfig)

        launchActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("owntracks:///config?url=http%3A%2F%2Flocalhost%3A8080%2Fnotfound")
            )
        )
        sleep(1000)
        assertContains(R.id.importError, "Unexpected status code")
        assertNotExist(R.id.save)
        assertDisplayed(R.id.close)
    }

    @Test
    fun loadActivityCanLoadConfigFromFileURL() {
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        val localConfig = File(dir, "espresso-testconfig.otrc")
        localConfig.writeText(servedConfig)
        launchActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("file://${localConfig.absoluteFile}")
            )
        )
        assertContains(R.id.effectiveConfiguration, expectedConfig)
        assertDisplayed(R.id.save)
        assertDisplayed(R.id.close)
    }

    @Test
    fun loadActivityCanLoadConfigFromContentURL() {
        launchActivity(
            Intent(
                Intent.ACTION_VIEW,
                writeFileToDevice("espresso-testconfig.otrc", servedConfig.toByteArray())
            )
        )
        assertContains(R.id.effectiveConfiguration, expectedConfig)
        assertDisplayed(R.id.save)
        assertDisplayed(R.id.close)
    }

    @Test
    fun loadActivityErrorsCorrectlyFromInvalidContentURL() {
        launchActivity(Intent(Intent.ACTION_VIEW, null))
        assertContains(R.id.importError, "Import failed: No URI given for importing configuration")
        assertNotExist(R.id.save)
        assertDisplayed(R.id.close)
    }

    class MockWebserverConfigDispatcher(private val config: String) : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val errorResponse = MockResponse().setResponseCode(404)
            return if (request.path == "/myconfig.otrc") {
                MockResponse().setResponseCode(200)
                    .setHeader("Content-type", "application/json")
                    .setBody(config)
            } else {
                errorResponse
            }
        }
    }
}
