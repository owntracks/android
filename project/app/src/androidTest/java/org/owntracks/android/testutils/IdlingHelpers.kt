package org.owntracks.android.testutils

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import timber.log.Timber


inline fun IdlingResource?.with(block: () -> Unit) {
    if (this == null) {
        Timber.w("Idling resource is null")
    }
    try {
        this?.run { IdlingRegistry.getInstance().register(this) }
        block()
    } finally {
        this?.run { IdlingRegistry.getInstance().unregister(this) }
    }
}