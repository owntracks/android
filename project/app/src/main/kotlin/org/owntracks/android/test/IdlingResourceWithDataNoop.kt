package org.owntracks.android.test

import androidx.test.espresso.IdlingResource
import org.owntracks.android.model.messages.MessageBase

class IdlingResourceWithDataNoop : IdlingResourceWithData<MessageBase> {
  override fun add(thing: MessageBase) {}

  override fun remove(thing: MessageBase) {}

  override fun reconcile() {}

  override fun getName(): String = ""

  override fun isIdleNow(): Boolean = true

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {}
}
