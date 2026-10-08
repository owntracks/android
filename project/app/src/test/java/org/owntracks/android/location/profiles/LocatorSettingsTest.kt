package org.owntracks.android.location.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.location.LocatorPriority
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode

class LocatorSettingsTest {
  private val preferences =
      mock<Preferences> {
        on { monitoring } doReturn MonitoringMode.Significant
        on { locatorInterval } doReturn 60
        on { moveModeLocatorInterval } doReturn 10
        on { locatorDisplacement } doReturn 500
        on { locatorPriority } doReturn null
        on { ping } doReturn 15
      }

  @Test
  fun `without overrides the settings are the preferences`() {
    assertEquals(
        LocatorSettings(MonitoringMode.Significant, 60, 10, 500, null, 15, null),
        LocatorSettings.from(preferences, null),
    )
  }

  @Test
  fun `empty overrides inherit every preference`() {
    assertEquals(
        LocatorSettings.from(preferences, null),
        LocatorSettings.from(preferences, LocatorOverrides()),
    )
  }

  @Test
  fun `set overrides replace the matching preferences only`() {
    val settings =
        LocatorSettings.from(
            preferences,
            LocatorOverrides(
                monitoring = MonitoringMode.Move,
                moveModeLocatorInterval = 5,
                locatorPriority = LocatorPriority.HighAccuracy,
            ),
        )
    assertEquals(
        LocatorSettings(MonitoringMode.Move, 60, 5, 500, LocatorPriority.HighAccuracy, 15, null),
        settings,
    )
  }

  @Test
  fun `static location is carried through`() {
    val staticLocation = StaticLocation(40.1, -111.6)
    assertEquals(
        staticLocation,
        LocatorSettings.from(preferences, LocatorOverrides(staticLocation = staticLocation))
            .staticLocation,
    )
  }

  @Test
  fun `out of range overrides are coerced to sane values`() {
    val settings =
        LocatorSettings.from(
            preferences,
            LocatorOverrides(
                locatorInterval = 0,
                moveModeLocatorInterval = -5,
                locatorDisplacement = -1,
                ping = 1,
            ),
        )
    assertEquals(1, settings.locatorInterval)
    assertEquals(1, settings.moveModeLocatorInterval)
    assertEquals(0, settings.locatorDisplacement)
    assertEquals(15, settings.ping)
  }

  @Test
  fun `no static location without a profile`() {
    assertNull(LocatorSettings.from(preferences, null).staticLocation)
  }
}
