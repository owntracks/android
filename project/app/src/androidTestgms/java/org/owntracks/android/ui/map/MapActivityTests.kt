package org.owntracks.android.ui.map

import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickBack
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import com.schibsted.spain.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.schibsted.spain.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.schibsted.spain.barista.interaction.BaristaEditTextInteractions.writeTo
import com.schibsted.spain.barista.rule.BaristaRule
import com.schibsted.spain.barista.rule.flaky.AllowFlaky
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.owntracks.android.R
import org.owntracks.android.ScreenshotTakingOnTestEndRule
import org.owntracks.android.support.Preferences
import org.owntracks.android.ui.preferences.clickOnAndWait

@LargeTest
@RunWith(AndroidJUnit4::class)
/* TODO the android orchestrator doesn't work with coverage, so until it does we need to run tests in order */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GMSMapActivityTests {
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
        assertDisplayed(R.id.google_map_view)

        openDrawer()
        clickOnAndWait(R.string.title_activity_preferences)
        clickOnAndWait(R.string.configurationManagement)
        Espresso.openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
        clickOn(R.string.preferencesEditor)
        writeTo(R.id.inputKey, baristaRule.activityTestRule.activity.getString(R.string.preferenceKeyExperimentalFeatures))
        writeTo(R.id.inputValue, Preferences.EXPERIMENTAL_FEATURE_USE_OSM_MAP)
        clickDialogPositiveButton()
        clickBack()
        clickBack()
        assertDisplayed(R.id.osm_map_view)
    }
}
