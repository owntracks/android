package org.owntracks.android.testutils.idlingresources

import androidx.test.espresso.IdlingResource

interface DispatcherWithIdlingResource {
  val idlingResource: IdlingResource
}
