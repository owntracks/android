package org.owntracks.android.e2e

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import com.schibsted.spain.barista.interaction.PermissionGranter
import com.schibsted.spain.barista.internal.util.resourceMatcher
import org.owntracks.android.App
import org.owntracks.android.R
import org.owntracks.android.ui.clickOnAndWait

internal fun doWelcomeProcess(app: App) {
    clickOnAndWait(R.id.btn_next)
    clickOnAndWait(R.id.btn_next)
    try {
        onView(R.id.fix_permissions_button.resourceMatcher()).check(
            matches(
                withEffectiveVisibility(
                    ViewMatchers.Visibility.VISIBLE
                )
            )
        )
        clickOn(R.id.fix_permissions_button)
        IdlingRegistry.getInstance().register(app.permissionIdlingResource)
        PermissionGranter.allowPermissionsIfNeeded(ACCESS_FINE_LOCATION)
        clickOnAndWait(R.id.btn_next)
    } catch (e: NoMatchingViewException) {

    }
    clickOnAndWait(R.id.done)
}

fun scrollToPreferenceWithText(textResource: Int) {
    onView(ViewMatchers.withId(androidx.preference.R.id.recycler_view))
        .perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                ViewMatchers.hasDescendant(ViewMatchers.withText(textResource)),
                ViewActions.scrollTo()
            )
        )
}