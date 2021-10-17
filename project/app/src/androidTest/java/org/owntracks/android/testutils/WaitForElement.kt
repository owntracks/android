package org.owntracks.android.testutils

import android.view.View
import android.view.ViewTreeObserver
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.StringDescription

// From https://stackoverflow.com/a/59149893/352740
fun waitForElement(matcher: Matcher<View>) {
    onView(ViewMatchers.isRoot()).perform(waitUntil(ViewMatchers.hasDescendant(matcher)))
}

private class ViewPropertyChangeCallback(
    private val matcher: Matcher<View>,
    private val view: View
) : IdlingResource, ViewTreeObserver.OnDrawListener {

    private lateinit var callback: IdlingResource.ResourceCallback
    private var matched = false

    override fun getName() = "View property change callback"

    override fun isIdleNow() = matched

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
    }

    override fun onDraw() {
        matched = matcher.matches(view)
        callback.onTransitionToIdle()
    }
}

fun waitUntil(matcher: Matcher<View>): ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> = Matchers.any(View::class.java)

    override fun getDescription(): String = StringDescription().let {
        matcher.describeTo(it)
        "wait until: $it"
    }

    override fun perform(uiController: UiController, view: View) {
        if (!matcher.matches(view)) {
            ViewPropertyChangeCallback(matcher, view).run {
                try {
                    IdlingRegistry.getInstance().register(this)
                    view.viewTreeObserver.addOnDrawListener(this)
                    uiController.loopMainThreadUntilIdle()
                } finally {
                    view.viewTreeObserver.removeOnDrawListener(this)
                    IdlingRegistry.getInstance().unregister(this)
                }
            }
        }
    }
}
