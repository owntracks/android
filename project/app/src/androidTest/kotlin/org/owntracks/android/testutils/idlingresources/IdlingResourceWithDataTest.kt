package org.owntracks.android.testutils.idlingresources

import org.junit.Test
import org.owntracks.android.model.messages.MessageLocation

class IdlingResourceWithDataTest {
  @Test
  fun given_an_Idling_Resource_when_adding_and_then_removing_the_same_item_then_the_resource_is_idle() {
    val ir = IdlingResourceWithDataImpl<MessageLocation>("test", compareBy { it.toString() })
    val message = MessageLocation()
    ir.add(message)
    ir.remove(message)
    assert(ir.isIdleNow)
  }

  @Test
  fun given_an_Idling_Resource_when_removing_and_then_adding_the_same_item_then_the_resource_is_idle() {
    val ir = IdlingResourceWithDataImpl<MessageLocation>("test", compareBy { it.toString() })
    val message = MessageLocation()
    ir.remove(message)
    ir.add(message)
    assert(ir.isIdleNow)
  }
}
