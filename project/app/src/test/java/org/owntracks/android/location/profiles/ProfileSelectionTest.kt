package org.owntracks.android.location.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileSelectionTest {
  private val home = ContextProfile("home", "Home", listOf(Condition.Charging), LocatorOverrides())
  private val car = ContextProfile("car", "Car", listOf(Condition.Charging), LocatorOverrides())

  @Test
  fun `a newly matched profile becomes effective`() {
    assertEquals(home, ProfileSelection().withMatched(home).effective)
  }

  @Test
  fun `a manual override suspends the active profile`() {
    val selection = ProfileSelection().withMatched(home).withManualOverride()
    assertEquals(home, selection.matched)
    assertNull(selection.effective)
  }

  @Test
  fun `the same profile matching again stays suspended`() {
    val selection = ProfileSelection().withMatched(home).withManualOverride().withMatched(home)
    assertNull(selection.effective)
  }

  @Test
  fun `an edited version of the suspended profile stays suspended`() {
    val edited = home.copy(name = "Home sweet home")
    val selection = ProfileSelection().withMatched(home).withManualOverride().withMatched(edited)
    assertEquals(edited, selection.matched)
    assertNull(selection.effective)
  }

  @Test
  fun `a different profile matching clears the suspension`() {
    val selection = ProfileSelection().withMatched(home).withManualOverride().withMatched(car)
    assertEquals(car, selection.effective)
  }

  @Test
  fun `no profile matching clears the suspension`() {
    val selection =
        ProfileSelection()
            .withMatched(home)
            .withManualOverride()
            .withMatched(null)
            .withMatched(home)
    assertEquals(home, selection.effective)
  }

  @Test
  fun `a manual override with no active profile does nothing`() {
    assertEquals(ProfileSelection(), ProfileSelection().withManualOverride())
  }

  @Test
  fun `a remembered suspension applies when its profile matches`() {
    assertNull(ProfileSelection(suspendedProfileId = "home").withMatched(home).effective)
  }

  @Test
  fun `a remembered suspension is dropped when a different profile matches`() {
    assertEquals(car, ProfileSelection(suspendedProfileId = "home").withMatched(car).effective)
  }
}
