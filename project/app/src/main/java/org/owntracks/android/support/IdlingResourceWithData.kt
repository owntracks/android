package org.owntracks.android.support

import androidx.test.espresso.IdlingResource

class IdlingResourceWithData<T>(private val resourceName: String) : IdlingResource {
    private var callback: IdlingResource.ResourceCallback? = null
    private val data = mutableSetOf<T>()
    override fun getName(): String =this.resourceName

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
    }

    override fun isIdleNow(): Boolean  = data.isEmpty()

    fun add(thing: T) {
        data.add(thing)
    }

    fun remove(thing: T) {
        data.remove(thing)
        if (data.isEmpty()) {
            callback?.onTransitionToIdle()
        }
    }
}
