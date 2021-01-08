package org.owntracks.android.ui

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.schibsted.spain.barista.rule.BaristaRule
import com.schibsted.spain.barista.rule.flaky.AllowFlaky
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.ScreenshotTakingOnTestEndRule
import org.owntracks.android.ui.preferences.load.LoadActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class LoadActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(LoadActivity::class.java)

    private val screenshotRule = ScreenshotTakingOnTestEndRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
            .outerRule(baristaRule.activityTestRule)
            .around(screenshotRule)

    val expectedConfig = """
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
  "debugLog" : true,
  "deviceId" : "testdevice",
  "fusedRegionDetection" : true,
  "geocodeEnabled" : true,
  "host" : "testhost.example.com",
  "ignoreInaccurateLocations" : 150,
  "ignoreStaleLocations" : 0,
  "keepalive" : 900,
  "locatorDisplacement" : 5,
  "locatorInterval" : 60,
  "locatorPriority" : 2,
  "mode" : 0,
  "monitoring" : 1,
  "moveModeLocatorInterval" : 10,
  "mqttProtocolLevel" : 3,
  "notificationHigherPriority" : false,
  "notificationLocation" : true,
  "opencageApiKey" : "",
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
}""".trimIndent()

    @Test
    @AllowFlaky(attempts = 1)
    fun loadActivityCanLoadConfigFromOwntracksInlineURI() {
        baristaRule.launchActivity(Intent(Intent.ACTION_VIEW, Uri.parse("owntracks:///config?inline=eyJfdHlwZSI6ImNvbmZpZ3VyYXRpb24iLCJ3YXlwb2ludHMiOlt7Il90eXBlIjoid2F5cG9pbnQiLCJkZXNjIjoid29yayIsImxhdCI6NTEuNSwibG9uIjotMC4wMiwicmFkIjoxNTAsInRzdCI6MTUwNTkxMDcwOTAwMH0seyJfdHlwZSI6IndheXBvaW50IiwiZGVzYyI6ImhvbWUiLCJsYXQiOjUzLjYsImxvbiI6LTEuNSwicmFkIjoxMDAsInRzdCI6MTU1ODM1MTI3M31dLCJhdXRoIjp0cnVlLCJhdXRvc3RhcnRPbkJvb3QiOnRydWUsImNsZWFuU2Vzc2lvbiI6ZmFsc2UsImNsaWVudElkIjoiZW11bGF0b3IiLCJjbWQiOnRydWUsImRlYnVnTG9nIjp0cnVlLCJkZXZpY2VJZCI6InRlc3RkZXZpY2UiLCJmdXNlZFJlZ2lvbkRldGVjdGlvbiI6dHJ1ZSwiZ2VvY29kZUVuYWJsZWQiOnRydWUsImhvc3QiOiJ0ZXN0aG9zdC5leGFtcGxlLmNvbSIsImlnbm9yZUluYWNjdXJhdGVMb2NhdGlvbnMiOjE1MCwiaWdub3JlU3RhbGVMb2NhdGlvbnMiOjAsImtlZXBhbGl2ZSI6OTAwLCJsb2NhdG9yRGlzcGxhY2VtZW50Ijo1LCJsb2NhdG9ySW50ZXJ2YWwiOjYwLCJsb2NhdG9yUHJpb3JpdHkiOjIsIm1vZGUiOjAsIm1vbml0b3JpbmciOjEsIm1vdmVNb2RlTG9jYXRvckludGVydmFsIjoxMCwibXF0dFByb3RvY29sTGV2ZWwiOjMsIm5vdGlmaWNhdGlvbkhpZ2hlclByaW9yaXR5IjpmYWxzZSwibm90aWZpY2F0aW9uTG9jYXRpb24iOnRydWUsIm9wZW5jYWdlQXBpS2V5IjoiIiwicGFzc3dvcmQiOiJwYXNzd29yZCIsInBpbmciOjMwLCJwb3J0IjoxODgzLCJwdWJFeHRlbmRlZERhdGEiOnRydWUsInB1YlFvcyI6MSwicHViUmV0YWluIjp0cnVlLCJwdWJUb3BpY0Jhc2UiOiJvd250cmFja3MvJXUvJWQiLCJyZW1vdGVDb25maWd1cmF0aW9uIjp0cnVlLCJzdWIiOnRydWUsInN1YlFvcyI6Miwic3ViVG9waWMiOiJvd250cmFja3MvKy8rIiwidGxzIjpmYWxzZSwidXNlUGFzc3dvcmQiOnRydWUsInVzZXJuYW1lIjoidXNlcm5hbWUiLCJ3cyI6ZmFsc2V9Cg==")))
        assertContains(R.id.effectiveConfiguration, expectedConfig)
    }
}