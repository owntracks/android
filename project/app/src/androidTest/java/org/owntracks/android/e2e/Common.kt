package org.owntracks.android.e2e

import android.view.View
import androidx.annotation.IdRes
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.owntracks.android.R
import org.owntracks.android.support.SimpleIdlingResource
import org.owntracks.android.ui.clickOnAndWait
import timber.log.Timber
import java.util.concurrent.TimeUnit

internal fun clickOnRegardlessOfVisibility(@IdRes id: Int) {
    onView(withId(id)).check(
        matches(
            CoreMatchers.allOf(
                isEnabled(),
                isClickable()
            )
        )
    ).perform(
        object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isEnabled() // no constraints, they are checked above
            }

            override fun getDescription(): String {
                return "click plus button"
            }

            override fun perform(uiController: UiController?, view: View) {
                view.performClick()
            }
        }
    )
}

fun reportLocationFromMap(locationIdlingResource: SimpleIdlingResource?) {
    openDrawer()
    clickOnAndWait(R.string.title_activity_map)
    IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)
    Timber.d("Waiting for location")
    locationIdlingResource?.run {
        IdlingRegistry.getInstance().register(locationIdlingResource)
    }
    clickOnAndWait(R.id.menu_mylocation)
    Timber.d("location now available")
    clickOnAndWait(R.id.menu_report)
}

fun scrollToPreferenceWithText(textResource: Int) {
    onView(withId(androidx.preference.R.id.recycler_view))
        .perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(textResource)),
                ViewActions.scrollTo()
            )
        )
}

fun setNotFirstStartPreferences() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putBoolean(context.getString(R.string.preferenceKeyFirstStart), false)
        .putBoolean(context.getString(R.string.preferenceKeySetupNotCompleted), false)
        .apply()

}
