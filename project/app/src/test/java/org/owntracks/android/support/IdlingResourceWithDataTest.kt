package org.owntracks.android.support

import org.junit.Test
import org.owntracks.android.model.messages.MessageLocation

class IdlingResourceWithDataTest {
  @Test
  fun `given an Idling Resource, when adding and then removing the same item, then the resource is idle`() {
    val ir = IdlingResourceWithData<MessageLocation>("test", compareBy { it.toString() })
    val message = MessageLocation()
    ir.add(message)
    ir.remove(message)
    assert(ir.isIdleNow)
  }

  @Test
  fun `given an Idling Resource, when removing and then adding the same item, then the resource is idle`() {
    val ir = IdlingResourceWithData<MessageLocation>("test", compareBy { it.toString() })
    val message = MessageLocation()
    ir.remove(message)
    ir.add(message)
    assert(ir.isIdleNow)
  }
}
