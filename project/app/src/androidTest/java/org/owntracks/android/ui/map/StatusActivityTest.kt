package org.owntracks.android.ui.map

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R

@LargeTest
@RunWith(AndroidJUnit4::class)
@Ignore
class StatusActivityTest {


    @Rule
    @JvmField
    var activityScenarioRule = ActivityScenarioRule(MapActivity::class.java).apply {
        val context = getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .clear()
                .putBoolean("firstStart", false)
                .putBoolean("setupNotCompleted", false)
                .apply()
        context.getSharedPreferences("org.owntracks.android.preferences.private", Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Rule
    @JvmField
    var grantPermissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION")

    @Test
    fun statusActivityLaunches() {
        onView(withContentDescription("Open")).check(matches(isDisplayed()))
        onView(withContentDescription("Open")).perform(click())
        onView(withText(R.string.title_activity_status)).perform(click())
        onView(withId(R.id.connectedStatus)).check(matches((isDisplayed())))
    }
}