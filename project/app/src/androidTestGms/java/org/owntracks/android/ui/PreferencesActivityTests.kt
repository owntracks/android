package org.owntracks.android.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.ui.preferences.PreferencesActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class PreferencesActivityTests :
    TestWithAnActivity<PreferencesActivity>(PreferencesActivity::class.java) {

    @Test
    fun defaultGeocoderIsSelected() {
        clickOnAndWait(R.string.preferencesAdvanced)
        scrollToText(R.string.preferencesReverseGeocodeProvider)
        assertDisplayed(baristaRule.activityTestRule.activity.resources.getStringArray(R.array.geocoders)[1])
    }

    private fun scrollToText(textResource: Int) {
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(textResource)), scrollTo()
                )
            )
    }
}