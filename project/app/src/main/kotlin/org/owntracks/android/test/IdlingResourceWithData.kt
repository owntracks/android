package org.owntracks.android.test

import androidx.test.espresso.IdlingResource
import org.owntracks.android.model.messages.MessageBase

interface IdlingResourceWithData<T : MessageBase> : IdlingResource {
  fun add(thing: T)

  fun remove(thing: T)

  fun reconcile()
}
