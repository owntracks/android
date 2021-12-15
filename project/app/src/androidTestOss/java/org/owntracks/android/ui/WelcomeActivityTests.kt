package org.owntracks.android.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import com.adevinta.android.barista.interaction.BaristaViewPagerInteractions.swipeViewPagerBack
import com.adevinta.android.barista.interaction.BaristaViewPagerInteractions.swipeViewPagerForward
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.ui.welcome.WelcomeActivity


@LargeTest
@RunWith(AndroidJUnit4::class)
class WelcomeActivityTests : TestWithAnActivity<WelcomeActivity>(WelcomeActivity::class.java) {
    @Test
    fun welcomeActivityCanBeSwipedThroughToTheEnd() {
        swipeViewPagerForward()
        swipeViewPagerForward()

        sleep(500)
        assertDisplayed(R.id.done)
    }

    @Test
    fun welcomeActivityCanBeSwipedBackToStart() {
        swipeViewPagerForward()
        swipeViewPagerBack()
        assertDisplayed(R.string.welcome_heading)
    }
}