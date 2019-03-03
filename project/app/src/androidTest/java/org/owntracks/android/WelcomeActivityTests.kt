package org.owntracks.android

import android.os.Build
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.LOLLIPOP_MR1
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.owntracks.android.ui.welcome.WelcomeActivity

@LargeTest
class WelcomeActivityTests {
    @get:Rule
    val activityTestRule = ActivityTestRule<WelcomeActivity>(WelcomeActivity::class.java, true, false)

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    /*
    Clear sharedPreferences to give a clean slate
     */
    @Before
    fun setUp() {

        val targetContext = getInstrumentation().targetContext
        val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(targetContext).edit()
        preferencesEditor.clear().commit()
        activityTestRule.launchActivity(null)
    }

    @Test
    fun welcomeScreenLoadsAndResponds() {
        assertTrue(Build.VERSION.SDK_INT in LOLLIPOP..LOLLIPOP_MR1)
        if (Build.VERSION.SDK_INT == LOLLIPOP_MR1) {
            onView(withId(R.id.btn_next)).perform(click())
            onView(withId(R.id.done)).perform(click())
        }
    }
}