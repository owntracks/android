package org.owntracks.android.ui.preferences

import org.junit.Assert.assertEquals
import org.junit.Test
import org.owntracks.android.location.profiles.Condition
import org.owntracks.android.location.profiles.ContextProfile
import org.owntracks.android.location.profiles.LocatorOverrides

class ProfileStatusTest {
  private val home = ContextProfile("home", "Home", listOf(Condition.AnyWifi), LocatorOverrides())

  @Test
  fun `profiles being off wins over anything matching`() {
    assertEquals(ProfileStatus.Off, profileStatus(false, home, home))
  }

  @Test
  fun `with nothing matching the normal settings are used`() {
    assertEquals(ProfileStatus.NoneApplies, profileStatus(true, null, null))
  }

  @Test
  fun `the applied profile is active`() {
    assertEquals(ProfileStatus.Active(home), profileStatus(true, home, home))
  }

  @Test
  fun `a matching profile that isn't applied has been paused by a manual change`() {
    assertEquals(ProfileStatus.Paused(home), profileStatus(true, home, null))
  }
}
