package org.owntracks.android.location.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.owntracks.android.location.LocatorPriority
import org.owntracks.android.preferences.types.MonitoringMode

class ContextProfileTest {
  private val home =
      ContextProfile(
          id = "home",
          name = "Home",
          conditions = listOf(Condition.WifiSsid("MyHome"), Condition.WifiSsid("MyHome-5G")),
          overrides = LocatorOverrides(staticLocation = StaticLocation(40.1, -111.6), ping = 30),
      )
  private val car =
      ContextProfile(
          id = "car",
          name = "Car",
          conditions = listOf(Condition.BluetoothDevice("AA:BB:CC:DD:EE:FF", "Car")),
          overrides =
              LocatorOverrides(
                  monitoring = MonitoringMode.Move,
                  locatorPriority = LocatorPriority.HighAccuracy,
              ),
      )
  private val charging =
      ContextProfile(
          id = "charging",
          name = "Charging",
          conditions = listOf(Condition.Charging),
          overrides = LocatorOverrides(monitoring = MonitoringMode.Move),
      )

  @Test
  fun `profiles survive a round trip through json`() {
    val profiles =
        listOf(
            home,
            car,
            charging,
            ContextProfile(
                "work",
                "Work",
                listOf(Condition.InRegion(1_700_000_000)),
                LocatorOverrides(),
            ),
        )
    assertEquals(profiles, decodeContextProfiles(encodeContextProfiles(profiles)))
  }

  @Test
  fun `a region condition is stored by the waypoint's timestamp`() {
    val json =
        encodeContextProfiles(
            listOf(ContextProfile("w", "W", listOf(Condition.InRegion(42)), LocatorOverrides()))
        )
    assertTrue(json.contains("{\"type\":\"region\",\"tst\":42}"))
  }

  @Test
  fun `monitoring mode is encoded as its config integer value`() {
    assertTrue(encodeContextProfiles(listOf(car)).contains("\"monitoring\":2"))
  }

  @Test
  fun `invalid json decodes to no profiles`() {
    assertEquals(emptyList<ContextProfile>(), decodeContextProfiles("{not json"))
  }

  @Test
  fun `empty string decodes to no profiles`() {
    assertEquals(emptyList<ContextProfile>(), decodeContextProfiles(""))
  }

  @Test
  fun `unknown keys are ignored when decoding`() {
    val decoded =
        decodeContextProfiles(
            """[{"id":"a","name":"A","conditions":[{"type":"charging"}],"overrides":{},"future":1}]"""
        )
    assertEquals(listOf(Condition.Charging), decoded.single().conditions)
  }

  @Test
  fun `wifi condition matches the connected ssid exactly`() {
    assertTrue(Condition.WifiSsid("MyHome").matches(DeviceContext(wifiSsid = "MyHome")))
    assertFalse(Condition.WifiSsid("MyHome").matches(DeviceContext(wifiSsid = "myhome")))
    assertFalse(Condition.WifiSsid("MyHome").matches(DeviceContext(wifiSsid = null)))
  }

  @Test
  fun `bluetooth condition matches any connected device address ignoring case`() {
    val context = DeviceContext(bluetoothDeviceAddresses = setOf("aa:bb:cc:dd:ee:ff"))
    assertTrue(Condition.BluetoothDevice("AA:BB:CC:DD:EE:FF", "Car").matches(context))
    assertFalse(Condition.BluetoothDevice("11:22:33:44:55:66", "Other").matches(context))
  }

  @Test
  fun `charging condition matches only while charging`() {
    assertTrue(Condition.Charging.matches(DeviceContext(chargingSource = ChargingSource.Cable)))
    assertFalse(Condition.Charging.matches(DeviceContext(chargingSource = null)))
  }

  @Test
  fun `region condition matches when inside that waypoint`() {
    assertTrue(Condition.InRegion(7).matches(DeviceContext(enteredWaypointTsts = setOf(3, 7))))
    assertFalse(Condition.InRegion(7).matches(DeviceContext(enteredWaypointTsts = setOf(3))))
  }

  @Test
  fun `a profile matches if any of its conditions match`() {
    assertEquals(home, listOf(home).firstMatching(DeviceContext(wifiSsid = "MyHome-5G")))
  }

  @Test
  fun `a profile with no conditions never matches`() {
    val empty = ContextProfile("e", "Empty", emptyList(), LocatorOverrides())
    assertNull(listOf(empty).firstMatching(DeviceContext(chargingSource = ChargingSource.Cable)))
  }

  @Test
  fun `the first matching profile in list order wins`() {
    val context = DeviceContext(wifiSsid = "MyHome", chargingSource = ChargingSource.Cable)
    assertEquals(home, listOf(home, charging).firstMatching(context))
    assertEquals(charging, listOf(charging, home).firstMatching(context))
  }

  @Test
  fun `disabled profiles are skipped`() {
    val context = DeviceContext(wifiSsid = "MyHome", chargingSource = ChargingSource.Cable)
    assertEquals(charging, listOf(home.copy(enabled = false), charging).firstMatching(context))
  }

  @Test
  fun `nothing matches an empty context`() {
    assertNull(listOf(home, car, charging).firstMatching(DeviceContext()))
  }

  private val wifiOrCharging = listOf(Condition.WifiSsid("MyHome"), Condition.Charging)

  private fun profileMatching(match: ConditionMatch, conditions: List<Condition> = wifiOrCharging) =
      ContextProfile("p", "P", conditions, LocatorOverrides(), match = match)

  @Test
  fun `profiles match on any condition by default`() {
    assertEquals(
        ConditionMatch.Any,
        ContextProfile("p", "P", emptyList(), LocatorOverrides()).match,
    )
  }

  @Test
  fun `an all profile matches only when every condition matches`() {
    val profiles = listOf(profileMatching(ConditionMatch.All))
    assertNull(profiles.firstMatching(DeviceContext(wifiSsid = "MyHome")))
    assertEquals(
        profiles.single(),
        profiles.firstMatching(
            DeviceContext(wifiSsid = "MyHome", chargingSource = ChargingSource.Cable)
        ),
    )
  }

  @Test
  fun `a none profile matches only when no condition matches`() {
    val profiles = listOf(profileMatching(ConditionMatch.None))
    assertEquals(profiles.single(), profiles.firstMatching(DeviceContext()))
    assertNull(profiles.firstMatching(DeviceContext(chargingSource = ChargingSource.Cable)))
  }

  @Test
  fun `a profile with no conditions never matches whatever the match mode`() {
    ConditionMatch.entries.forEach {
      assertNull(listOf(profileMatching(it, emptyList())).firstMatching(DeviceContext()))
    }
  }

  @Test
  fun `the match mode survives a round trip through json`() {
    val profiles = listOf(profileMatching(ConditionMatch.None))
    assertEquals(profiles, decodeContextProfiles(encodeContextProfiles(profiles)))
    assertTrue(encodeContextProfiles(profiles).contains("\"match\":\"none\""))
  }

  @Test
  fun `any wifi condition matches when connected to any wifi network`() {
    assertTrue(Condition.AnyWifi.matches(DeviceContext(wifiSsid = "Somewhere")))
    assertFalse(Condition.AnyWifi.matches(DeviceContext(wifiSsid = null)))
  }

  @Test
  fun `any wifi condition survives a round trip through json`() {
    val profiles = listOf(ContextProfile("w", "W", listOf(Condition.AnyWifi), LocatorOverrides()))
    assertEquals(profiles, decodeContextProfiles(encodeContextProfiles(profiles)))
  }

  @Test
  fun `charging from a source matches only that source`() {
    val wireless = Condition.ChargingFrom(ChargingSource.Wireless)
    assertTrue(wireless.matches(DeviceContext(chargingSource = ChargingSource.Wireless)))
    assertFalse(wireless.matches(DeviceContext(chargingSource = ChargingSource.Cable)))
    assertFalse(wireless.matches(DeviceContext()))
  }

  @Test
  fun `charging matches charging from any source`() {
    ChargingSource.entries.forEach {
      assertTrue(Condition.Charging.matches(DeviceContext(chargingSource = it)))
    }
  }

  @Test
  fun `charging from a source survives a round trip through json`() {
    val profiles =
        listOf(
            ContextProfile(
                "c",
                "C",
                listOf(Condition.ChargingFrom(ChargingSource.Dock)),
                LocatorOverrides(),
            )
        )
    assertEquals(profiles, decodeContextProfiles(encodeContextProfiles(profiles)))
  }

  @Test
  fun `the charging source is worked out from how the device is plugged in`() {
    assertNull(chargingSourceFromPlugged(0))
    assertEquals(ChargingSource.Cable, chargingSourceFromPlugged(1)) // AC
    assertEquals(ChargingSource.Cable, chargingSourceFromPlugged(2)) // USB
    assertEquals(ChargingSource.Wireless, chargingSourceFromPlugged(4))
    assertEquals(ChargingSource.Dock, chargingSourceFromPlugged(8))
  }
}
