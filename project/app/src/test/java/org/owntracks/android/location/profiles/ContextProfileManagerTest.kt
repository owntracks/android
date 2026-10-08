package org.owntracks.android.location.profiles

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.owntracks.android.location.LocatorPriority
import org.owntracks.android.preferences.InMemoryPreferencesStore
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode

@OptIn(ExperimentalCoroutinesApi::class)
class ContextProfileManagerTest {
  /** Nothing is known about the device until a context is emitted */
  private class FakeDeviceContextProvider : DeviceContextProvider {
    override val deviceContext = MutableSharedFlow<DeviceContext>(replay = 1)
  }

  private val home =
      ContextProfile(
          "home",
          "Home",
          listOf(Condition.WifiSsid("MyHome")),
          LocatorOverrides(staticLocation = StaticLocation(40.1, -111.6)),
      )
  private val car =
      ContextProfile(
          "car",
          "Car",
          listOf(Condition.Charging),
          LocatorOverrides(
              monitoring = MonitoringMode.Move,
              locatorPriority = LocatorPriority.HighAccuracy,
          ),
      )

  private val preferences =
      Preferences(InMemoryPreferencesStore(), mock()).apply {
        monitoring = MonitoringMode.Significant
        contextProfiles = encodeContextProfiles(listOf(home, car))
        contextProfilesEnabled = true
      }
  private val deviceContextProvider = FakeDeviceContextProvider()

  private fun TestScope.buildManager() =
      ContextProfileManager(preferences, deviceContextProvider, backgroundScope)

  @Test
  fun `with no matching profile the settings are the preferences`() =
      runTest(UnconfinedTestDispatcher()) {
        val manager = buildManager()
        assertNull(manager.activeProfile.value)
        assertEquals(LocatorSettings.from(preferences, null), manager.locatorSettings.value)
      }

  @Test
  fun `a matching profile's overrides are applied`() =
      runTest(UnconfinedTestDispatcher()) {
        val manager = buildManager()
        deviceContextProvider.deviceContext.tryEmit(
            DeviceContext(chargingSource = ChargingSource.Cable)
        )
        assertEquals(car, manager.activeProfile.value)
        assertEquals(MonitoringMode.Move, manager.locatorSettings.value.monitoring)
        assertEquals(LocatorPriority.HighAccuracy, manager.locatorSettings.value.locatorPriority)
      }

  @Test
  fun `the settings revert when the profile stops matching`() =
      runTest(UnconfinedTestDispatcher()) {
        val manager = buildManager()
        deviceContextProvider.deviceContext.tryEmit(
            DeviceContext(chargingSource = ChargingSource.Cable)
        )
        deviceContextProvider.deviceContext.tryEmit(DeviceContext(chargingSource = null))
        assertNull(manager.activeProfile.value)
        assertEquals(MonitoringMode.Significant, manager.locatorSettings.value.monitoring)
      }

  @Test
  fun `profiles are ignored while the feature is disabled`() =
      runTest(UnconfinedTestDispatcher()) {
        preferences.contextProfilesEnabled = false
        val manager = buildManager()
        deviceContextProvider.deviceContext.tryEmit(
            DeviceContext(chargingSource = ChargingSource.Cable)
        )
        assertNull(manager.activeProfile.value)
        assertEquals(MonitoringMode.Significant, manager.locatorSettings.value.monitoring)
      }

  @Test
  fun `enabling the feature applies the matching profile`() =
      runTest(UnconfinedTestDispatcher()) {
        preferences.contextProfilesEnabled = false
        val manager = buildManager()
        deviceContextProvider.deviceContext.tryEmit(
            DeviceContext(chargingSource = ChargingSource.Cable)
        )
        preferences.contextProfilesEnabled = true
        assertEquals(car, manager.activeProfile.value)
      }

  @Test
  fun `editing the profiles reapplies them`() =
      runTest(UnconfinedTestDispatcher()) {
        val manager = buildManager()
        deviceContextProvider.deviceContext.tryEmit(
            DeviceContext(chargingSource = ChargingSource.Cable)
        )
        preferences.contextProfiles =
            encodeContextProfiles(
                listOf(car.copy(overrides = LocatorOverrides(monitoring = MonitoringMode.Quiet)))
            )
        assertEquals(MonitoringMode.Quiet, manager.locatorSettings.value.monitoring)
      }

  @Test
  fun `a manual monitoring change wins over the active profile`() =
      runTest(UnconfinedTestDispatcher()) {
        val manager = buildManager()
        deviceContextProvider.deviceContext.tryEmit(
            DeviceContext(chargingSource = ChargingSource.Cable)
        )
        preferences.monitoring = MonitoringMode.Manual
        assertNull(manager.activeProfile.value)
        assertEquals(MonitoringMode.Manual, manager.locatorSettings.value.monitoring)
      }

  @Test
  fun `automation resumes after the context changes following a manual change`() =
      runTest(UnconfinedTestDispatcher()) {
        val manager = buildManager()
        deviceContextProvider.deviceContext.tryEmit(
            DeviceContext(chargingSource = ChargingSource.Cable)
        )
        preferences.monitoring = MonitoringMode.Manual
        deviceContextProvider.deviceContext.tryEmit(
            DeviceContext(wifiSsid = "MyHome", chargingSource = ChargingSource.Cable)
        )
        assertEquals(home, manager.activeProfile.value)
        assertEquals(StaticLocation(40.1, -111.6), manager.locatorSettings.value.staticLocation)
      }

  @Test
  fun `a base preference change with no active profile is applied directly`() =
      runTest(UnconfinedTestDispatcher()) {
        val manager = buildManager()
        preferences.locatorInterval = 120
        assertEquals(120, manager.locatorSettings.value.locatorInterval)
      }

  @Test
  fun `manually choosing the mode that is already the preference still overrides the profile`() =
      runTest(UnconfinedTestDispatcher()) {
        val manager = buildManager()
        deviceContextProvider.deviceContext.tryEmit(
            DeviceContext(chargingSource = ChargingSource.Cable)
        )
        manager.setMonitoringModeManually(MonitoringMode.Significant)
        assertNull(manager.activeProfile.value)
        assertEquals(MonitoringMode.Significant, manager.locatorSettings.value.monitoring)
      }

  @Test
  fun `manually choosing a mode sets the preference`() =
      runTest(UnconfinedTestDispatcher()) {
        val manager = buildManager()
        manager.setMonitoringModeManually(MonitoringMode.Quiet)
        assertEquals(MonitoringMode.Quiet, preferences.monitoring)
      }

  @Test
  fun `the matched profile is still known while a manual change overrides it`() =
      runTest(UnconfinedTestDispatcher()) {
        val manager = buildManager()
        deviceContextProvider.deviceContext.tryEmit(
            DeviceContext(chargingSource = ChargingSource.Cable)
        )
        preferences.monitoring = MonitoringMode.Manual
        assertEquals(car, manager.matchedProfile.value)
        assertNull(manager.activeProfile.value)
      }

  @Test
  fun `nothing is matched when no profile applies`() =
      runTest(UnconfinedTestDispatcher()) {
        val manager = buildManager()
        assertNull(manager.matchedProfile.value)
      }

  @Test
  fun `the device isn't watched while profiles are off`() =
      runTest(UnconfinedTestDispatcher()) {
        preferences.contextProfilesEnabled = false
        buildManager()
        assertEquals(0, deviceContextProvider.deviceContext.subscriptionCount.value)
      }

  @Test
  fun `turning profiles off stops watching the device`() =
      runTest(UnconfinedTestDispatcher()) {
        buildManager()
        assertEquals(1, deviceContextProvider.deviceContext.subscriptionCount.value)
        preferences.contextProfilesEnabled = false
        assertEquals(0, deviceContextProvider.deviceContext.subscriptionCount.value)
      }

  @Test
  fun `profiles aren't ready until the device is known`() =
      runTest(UnconfinedTestDispatcher()) {
        val manager = buildManager()
        assertFalse(manager.ready.value)
        deviceContextProvider.deviceContext.tryEmit(DeviceContext())
        assertTrue(manager.ready.value)
      }

  @Test
  fun `profiles are ready straight away while off`() =
      runTest(UnconfinedTestDispatcher()) {
        preferences.contextProfilesEnabled = false
        assertTrue(buildManager().ready.value)
      }

  @Test
  fun `no profile is matched before the device is known`() =
      runTest(UnconfinedTestDispatcher()) {
        val none =
            ContextProfile(
                "none",
                "None",
                listOf(Condition.AnyWifi),
                LocatorOverrides(),
                match = ConditionMatch.None,
            )
        preferences.contextProfiles = encodeContextProfiles(listOf(none))
        val manager = buildManager()
        assertNull(manager.matchedProfile.value)
        deviceContextProvider.deviceContext.tryEmit(DeviceContext())
        assertEquals(none, manager.matchedProfile.value)
      }

  @Test
  fun `a manual change survives a restart`() =
      runTest(UnconfinedTestDispatcher()) {
        buildManager().run {
          deviceContextProvider.deviceContext.tryEmit(
              DeviceContext(chargingSource = ChargingSource.Cable)
          )
          setMonitoringModeManually(MonitoringMode.Manual)
        }
        val restarted = buildManager()
        assertEquals(car, restarted.matchedProfile.value)
        assertNull(restarted.activeProfile.value)
      }

  @Test
  fun `changing a setting the active profile doesn't override doesn't pause it`() =
      runTest(UnconfinedTestDispatcher()) {
        val manager = buildManager()
        deviceContextProvider.deviceContext.tryEmit(
            DeviceContext(chargingSource = ChargingSource.Cable)
        )
        preferences.locatorDisplacement = 50
        assertEquals(car, manager.activeProfile.value)
      }

  @Test
  fun `changing a setting the active profile overrides pauses it`() =
      runTest(UnconfinedTestDispatcher()) {
        val manager = buildManager()
        deviceContextProvider.deviceContext.tryEmit(
            DeviceContext(chargingSource = ChargingSource.Cable)
        )
        preferences.locatorPriority = LocatorPriority.LowPower
        assertNull(manager.activeProfile.value)
      }
}
