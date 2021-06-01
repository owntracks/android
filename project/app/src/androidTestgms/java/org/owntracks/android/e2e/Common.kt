package org.owntracks.android.e2e

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions
import com.schibsted.spain.barista.interaction.PermissionGranter
import com.schibsted.spain.barista.internal.util.resourceMatcher
import org.owntracks.android.R
import org.owntracks.android.ui.clickOnAndWait

internal fun doWelcomeProcess() {
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
        clickOnAndWait(R.id.fix_permissions_button)
        PermissionGranter.allowPermissionsIfNeeded(ACCESS_FINE_LOCATION)
        BaristaSleepInteractions.sleep(1000)
        clickOnAndWait(R.id.btn_next)
    } catch (e: NoMatchingViewException) {

    }
    clickOnAndWait(R.id.done)
}