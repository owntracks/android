package org.owntracks.android.ui

import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import org.hamcrest.Matchers

fun assertContainsError(@IdRes id: Int, errorText: String) {
    onView(ViewMatchers.withId(id)).check(
        matches(
            hasErrorText(
                Matchers.containsString(errorText)
            )
        )
    )
}