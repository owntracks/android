package org.owntracks.android.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.owntracks.android.model.messages.MessageLocation

class ContactTest {
  private fun locationMessage(timestamp: Long, inregions: List<String>? = null) =
      MessageLocation().apply {
        this.timestamp = timestamp
        this.trackerId = "ab"
        this.inregions = inregions
      }

  @Test
  fun `contact stores the regions from a location message`() {
    val contact = Contact("test")
    assertTrue(
        contact.setLocationFromMessageLocation(locationMessage(1000, listOf("home", "work")))
    )
    assertEquals(listOf("home", "work"), contact.inregions)
    assertEquals("home, work", contact.inRegionsDisplay)
  }

  @Test
  fun `contact with no regions has no regions display text`() {
    val contact = Contact("test")
    contact.setLocationFromMessageLocation(locationMessage(1000, null))
    assertTrue(contact.inregions.isEmpty())
    assertNull(contact.inRegionsDisplay)
  }

  @Test
  fun `an older location message does not overwrite the regions`() {
    val contact = Contact("test")
    contact.setLocationFromMessageLocation(locationMessage(2000, listOf("home")))
    assertFalse(contact.setLocationFromMessageLocation(locationMessage(1000, listOf("work"))))
    assertEquals(listOf("home"), contact.inregions)
  }
}
