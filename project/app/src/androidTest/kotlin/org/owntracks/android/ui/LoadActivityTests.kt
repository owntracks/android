package org.owntracks.android.ui

import android.content.Intent
import android.net.Uri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.getText
import org.owntracks.android.testutils.idlingresources.ViewIdlingResource
import org.owntracks.android.testutils.use
import org.owntracks.android.testutils.writeFileToDevice
import org.owntracks.android.ui.preferences.load.LoadActivity

@MediumTest
@HiltAndroidTest
class LoadActivityTests : TestWithAnActivity<LoadActivity>(false) {

  private var mockWebServer = MockWebServer()

  @After
  fun teardown() {
    mockWebServer.shutdown()
  }

  // language=JSON
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
  "extendedData": true,
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

  private fun assertExpectedConfig(input: String) {
    val json = Json.parseToJsonElement(input).jsonObject
    assertEquals("configuration", json["_type"]!!.jsonPrimitive.content)
    assertEquals(2, json["waypoints"]!!.jsonArray.size)
    assertEquals(
        "work", json["waypoints"]!!.jsonArray[0].jsonObject["desc"]!!.jsonPrimitive.content)
    assertEquals(
        51.5, json["waypoints"]!!.jsonArray[0].jsonObject["lat"]!!.jsonPrimitive.double, 0.0001)
    assertEquals(
        -0.02, json["waypoints"]!!.jsonArray[0].jsonObject["lon"]!!.jsonPrimitive.double, 0.0001)
    assertEquals(150, json["waypoints"]!!.jsonArray[0].jsonObject["rad"]!!.jsonPrimitive.int)
    assertEquals(
        1505910709000, json["waypoints"]!!.jsonArray[0].jsonObject["tst"]!!.jsonPrimitive.long)
    assertEquals(
        "home", json["waypoints"]!!.jsonArray[1].jsonObject["desc"]!!.jsonPrimitive.content)
    assertEquals(
        53.6, json["waypoints"]!!.jsonArray[1].jsonObject["lat"]!!.jsonPrimitive.double, 0.0001)
    assertEquals(
        -1.5, json["waypoints"]!!.jsonArray[1].jsonObject["lon"]!!.jsonPrimitive.double, 0.0001)
    assertEquals(100, json["waypoints"]!!.jsonArray[1].jsonObject["rad"]!!.jsonPrimitive.int)
    assertEquals(
        1558351273, json["waypoints"]!!.jsonArray[1].jsonObject["tst"]!!.jsonPrimitive.long)
    assertTrue(json["auth"]!!.jsonPrimitive.boolean)
    assertTrue(json["autostartOnBoot"]!!.jsonPrimitive.boolean)
    assertFalse(json["cleanSession"]!!.jsonPrimitive.boolean)
    assertEquals("emulator", json["clientId"]!!.jsonPrimitive.content)
    assertTrue(json["cmd"]!!.jsonPrimitive.boolean)
    assertEquals(34, json["connectionTimeoutSeconds"]!!.jsonPrimitive.int)
    assertTrue(json["debugLog"]!!.jsonPrimitive.boolean)
    assertEquals("testdevice", json["deviceId"]!!.jsonPrimitive.content)
    assertFalse(json["enableMapRotation"]!!.jsonPrimitive.boolean)
    assertTrue(json["fusedRegionDetection"]!!.jsonPrimitive.boolean)
    assertTrue(json["geocodeEnabled"]!!.jsonPrimitive.boolean)
    assertEquals("testhost.example.com", json["host"]!!.jsonPrimitive.content)
    assertEquals(150, json["ignoreInaccurateLocations"]!!.jsonPrimitive.int)
    assertEquals(0, json["ignoreStaleLocations"]!!.jsonPrimitive.int)
    assertEquals(900, json["keepalive"]!!.jsonPrimitive.int)
    assertEquals(5, json["locatorDisplacement"]!!.jsonPrimitive.int)
    assertEquals(60, json["locatorInterval"]!!.jsonPrimitive.int)
    assertEquals(0, json["mode"]!!.jsonPrimitive.int)
    assertEquals(1, json["monitoring"]!!.jsonPrimitive.int)
    assertEquals(10, json["moveModeLocatorInterval"]!!.jsonPrimitive.int)
    assertEquals(3, json["mqttProtocolLevel"]!!.jsonPrimitive.int)
    assertFalse(json["notificationHigherPriority"]!!.jsonPrimitive.boolean)
    assertTrue(json["notificationLocation"]!!.jsonPrimitive.boolean)
    assertEquals("", json["opencageApiKey"]!!.jsonPrimitive.content)
    assertEquals(3.352, json["osmTileScaleFactor"]!!.jsonPrimitive.double, 0.0001)
    assertEquals("password", json["password"]!!.jsonPrimitive.content)
    assertEquals(30, json["ping"]!!.jsonPrimitive.int)
    assertEquals(1883, json["port"]!!.jsonPrimitive.int)
    assertTrue(json["extendedData"]!!.jsonPrimitive.boolean)
    assertEquals(1, json["pubQos"]!!.jsonPrimitive.int)
    assertTrue(json["pubRetain"]!!.jsonPrimitive.boolean)
    assertEquals("owntracks/%u/%d", json["pubTopicBase"]!!.jsonPrimitive.content)
    assertTrue(json["remoteConfiguration"]!!.jsonPrimitive.boolean)
    assertTrue(json["sub"]!!.jsonPrimitive.boolean)
    assertEquals(2, json["subQos"]!!.jsonPrimitive.int)
    assertEquals("owntracks/+/+", json["subTopic"]!!.jsonPrimitive.content)
  }

  @Test
  fun load_activity_can_load_config_from_owntracks_inline_config_url() {
    launchActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "owntracks:///config?inline=eyJfdHlwZSI6ImNvbmZpZ3VyYXRpb24iLCJ3YXlwb2ludHMiOlt7Il90eXBlIjoid2F5cG9pbnQiLCJkZXNjIjoid29yayIsImxhdCI6NTEuNSwibG9uIjotMC4wMiwicmFkIjoxNTAsInRzdCI6MTUwNTkxMDcwOTAwMH0seyJfdHlwZSI6IndheXBvaW50IiwiZGVzYyI6ImhvbWUiLCJsYXQiOjUzLjYsImxvbiI6LTEuNSwicmFkIjoxMDAsInRzdCI6MTU1ODM1MTI3M31dLCJhdXRoIjp0cnVlLCJhdXRvc3RhcnRPbkJvb3QiOnRydWUsImNvbm5lY3Rpb25UaW1lb3V0U2Vjb25kcyI6MzQsImNsZWFuU2Vzc2lvbiI6ZmFsc2UsImNsaWVudElkIjoiZW11bGF0b3IiLCJjbWQiOnRydWUsImRlYnVnTG9nIjp0cnVlLCJkZXZpY2VJZCI6InRlc3RkZXZpY2UiLCJmdXNlZFJlZ2lvbkRldGVjdGlvbiI6dHJ1ZSwiZ2VvY29kZUVuYWJsZWQiOnRydWUsImhvc3QiOiJ0ZXN0aG9zdC5leGFtcGxlLmNvbSIsImlnbm9yZUluYWNjdXJhdGVMb2NhdGlvbnMiOjE1MCwiaWdub3JlU3RhbGVMb2NhdGlvbnMiOjAsImtlZXBhbGl2ZSI6OTAwLCJsb2NhdG9yRGlzcGxhY2VtZW50Ijo1LCJsb2NhdG9ySW50ZXJ2YWwiOjYwLCJtb2RlIjowLCJtb25pdG9yaW5nIjoxLCJlbmFibGVNYXBSb3RhdGlvbiI6ZmFsc2UsIm9zbVRpbGVTY2FsZUZhY3RvciI6My4zNTIsIm1vdmVNb2RlTG9jYXRvckludGVydmFsIjoxMCwibXF0dFByb3RvY29sTGV2ZWwiOjMsIm5vdGlmaWNhdGlvbkhpZ2hlclByaW9yaXR5IjpmYWxzZSwibm90aWZpY2F0aW9uTG9jYXRpb24iOnRydWUsIm9wZW5jYWdlQXBpS2V5IjoiIiwicGFzc3dvcmQiOiJwYXNzd29yZCIsInBpbmciOjMwLCJwb3J0IjoxODgzLCJleHRlbmRlZERhdGEiOnRydWUsInB1YlFvcyI6MSwicHViUmV0YWluIjp0cnVlLCJwdWJUb3BpY0Jhc2UiOiJvd250cmFja3MvJXUvJWQiLCJyZW1vdGVDb25maWd1cmF0aW9uIjp0cnVlLCJzdWIiOnRydWUsInN1YlFvcyI6Miwic3ViVG9waWMiOiJvd250cmFja3MvKy8rIiwidGxzIjpmYWxzZSwidXNlUGFzc3dvcmQiOnRydWUsInVzZXJuYW1lIjoidXNlcm5hbWUiLCJ3cyI6ZmFsc2V9")))
    assertExpectedConfig(getText(onView(withId(R.id.effectiveConfiguration))))
    assertDisplayed(R.id.save)
    assertDisplayed(R.id.close)
  }

  @Test
  fun load_activity_shows_error_when_loading_from_inline_config_url_containing_invalid_json() {
    launchActivity(Intent(Intent.ACTION_VIEW, Uri.parse("owntracks:///config?inline=e30k")))
    assertContains(
        R.id.importError,
        app.getString(
            R.string.errorPreferencesImportFailed, "Message is not a valid configuration message"))
    assertNotExist(R.id.save)
    assertDisplayed(R.id.close)
  }

  @Test
  fun load_activity_shows_error_when_loading_from_inline_config_url_containing_invalid_base64() {
    launchActivity(
        Intent(
            Intent.ACTION_VIEW, Uri.parse("owntracks:///config?inline=aaaaaaaaaaaaaaaaaaaaaaaaa")))
    assertContains(R.id.importError, app.getString(R.string.errorPreferencesImportFailed, ""))
    assertNotExist(R.id.save)
    assertDisplayed(R.id.close)
  }

  @Test
  fun load_activity_can_load_config_from_owntracks_remote_url() {
    mockWebServer.start(8080)
    mockWebServer.dispatcher = MockWebserverConfigDispatcher(servedConfig)

    launchActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("owntracks:///config?url=http%3A%2F%2Flocalhost%3A8080%2Fmyconfig.otrc")))
    ViewIdlingResource(withId(R.id.effectiveConfiguration), isDisplayed()).use {
      assertExpectedConfig(getText(onView(withId(R.id.effectiveConfiguration))))
    }
    assertDisplayed(R.id.save)
    assertDisplayed(R.id.close)
  }

  @Test
  fun load_activity_shows_error_trying_to_load_not_found_remote_url() {
    mockWebServer.start(8080)
    mockWebServer.dispatcher = MockWebserverConfigDispatcher(servedConfig)

    launchActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("owntracks:///config?url=http%3A%2F%2Flocalhost%3A8080%2Fnotfound")))
    assertContains(R.id.importError, "Unexpected status code")
    assertNotExist(R.id.save)
    assertDisplayed(R.id.close)
  }

  @Test
  fun load_activity_can_load_config_from_file_url() {
    val dir = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
    val localConfig = File(dir, "espresso-testconfig.otrc")
    localConfig.writeText(servedConfig)
    launchActivity(Intent(Intent.ACTION_VIEW, Uri.parse("file://${localConfig.absoluteFile}")))
    ViewIdlingResource(withId(R.id.effectiveConfiguration), isDisplayed()).use {
      assertExpectedConfig(getText(onView(withId(R.id.effectiveConfiguration))))
    }
    assertExpectedConfig(getText(onView(withId(R.id.effectiveConfiguration))))
    assertDisplayed(R.id.save)
    assertDisplayed(R.id.close)
  }

  @Test
  fun load_activity_can_load_config_from_content_url() {
    launchActivity(
        Intent(
            Intent.ACTION_VIEW,
            writeFileToDevice("espresso-testconfig.otrc", servedConfig.toByteArray())))
    ViewIdlingResource(withId(R.id.effectiveConfiguration), isDisplayed()).use {
      assertExpectedConfig(getText(onView(withId(R.id.effectiveConfiguration))))
    }
    assertExpectedConfig(getText(onView(withId(R.id.effectiveConfiguration))))
    assertDisplayed(R.id.save)
    assertDisplayed(R.id.close)
  }

  @Test
  fun load_activity_errors_correctly_from_invalid_content_url() {
    launchActivity(Intent(Intent.ACTION_VIEW, null))
    assertContains(R.id.importError, "Import failed: No URI given for importing configuration")
    assertNotExist(R.id.save)
    assertDisplayed(R.id.close)
  }

  class MockWebserverConfigDispatcher(private val config: String) : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
      val errorResponse = MockResponse().setResponseCode(404)
      return if (request.path == "/myconfig.otrc") {
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-type", "application/json")
            .setBody(config)
      } else {
        errorResponse
      }
    }
  }
}
