package org.owntracks.android.ui.map

import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.rule.BaristaRule
import com.schibsted.spain.barista.rule.flaky.AllowFlaky
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.ScreenshotTakingOnTestEndRule

@LargeTest
@RunWith(AndroidJUnit4::class)
class OSSMapActivityTests {
    @get:Rule
    var baristaRule = BaristaRule.create(MapActivity::class.java)

    private val screenshotRule = ScreenshotTakingOnTestEndRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
            .outerRule(baristaRule.activityTestRule)
            .around(screenshotRule)

    @Test
    @AllowFlaky(attempts = 1)
    fun welcomeActivityShouldNotRunWhenFirstStartPreferencesSet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(context.getString(R.string.preferenceKeyFirstStart), false)
                .putBoolean(context.getString(R.string.preferenceKeySetupNotCompleted), false)
                .apply()
        baristaRule.launchActivity()
        assertDisplayed(R.id.osm_map_view)
    }
}
