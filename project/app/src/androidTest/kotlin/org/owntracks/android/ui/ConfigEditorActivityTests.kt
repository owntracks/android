package org.owntracks.android.ui

import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.filters.MediumTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.CoreMatchers.allOf
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.ui.preferences.editor.EditorActivity

@MediumTest
@HiltAndroidTest
class ConfigEditorActivityTests : TestWithAnActivity<EditorActivity>(startActivity = false) {

  @Test
  fun configuration_management_can_edit_a_set_type() {
    launchActivity()
    openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
    clickOn(R.string.preferencesEditor)
    writeTo(R.id.inputKey, Preferences::experimentalFeatures.name)
    writeTo(R.id.inputValue, "this, that,    other")
    clickDialogPositiveButton()
    assertContains(
        R.id.effectiveConfiguration, """"experimentalFeatures" : [ "other", "that", "this" ]""")
  }

  @Test
  fun configuration_management_can_edit_the_mode() {
    launchActivity()
    openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
    clickOn(R.string.preferencesEditor)
    writeTo(R.id.inputKey, Preferences::mode.name)
    writeTo(R.id.inputValue, "0")
    clickDialogPositiveButton()
    assertContains(R.id.effectiveConfiguration, """"mode" : 0""")
  }

  @Test
  fun configuration_management_can_edit_the_mode_to_a_default() {
    launchActivity()
    openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
    clickOn(R.string.preferencesEditor)
    writeTo(R.id.inputKey, Preferences::mode.name)
    writeTo(R.id.inputValue, "not a number")
    clickDialogPositiveButton()
    assertContains(R.id.effectiveConfiguration, """"mode" : 0""")
  }

  @Test
  fun configuration_management_can_edit_the_mqtt_protocol_level() {
    launchActivity()
    openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
    clickOn(R.string.preferencesEditor)
    writeTo(R.id.inputKey, Preferences::mqttProtocolLevel.name)
    writeTo(R.id.inputValue, "4")
    clickDialogPositiveButton()
    assertContains(R.id.effectiveConfiguration, """"mqttProtocolLevel" : 4""")
  }

  @Test
  fun configuration_management_can_edit_the_mqtt_protocol_level_to_a_default() {
    launchActivity()
    openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
    clickOn(R.string.preferencesEditor)
    writeTo(R.id.inputKey, Preferences::mqttProtocolLevel.name)
    writeTo(R.id.inputValue, "not a number")
    clickDialogPositiveButton()
    assertContains(R.id.effectiveConfiguration, """"mqttProtocolLevel" : 3""")
  }

  @Test
  fun configuration_management_can_edit_a_string_type() {
    launchActivity()
    openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
    clickOn(R.string.preferencesEditor)
    writeTo(R.id.inputKey, Preferences::host.name)
    writeTo(R.id.inputValue, "example.com")
    clickDialogPositiveButton()

    assertContains(R.id.effectiveConfiguration, """"host" : "example.com"""")
  }

  @Test
  fun configuration_management_can_edit_a_boolean_type() {
    launchActivity()
    openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
    clickOn(R.string.preferencesEditor)
    writeTo(R.id.inputKey, Preferences::cmd.name)
    writeTo(R.id.inputValue, "false")
    clickDialogPositiveButton()
    assertContains(R.id.effectiveConfiguration, """"cmd" : false""")
  }

  @Test
  fun configuration_management_can_edit_a_float_type() {
    launchActivity()
    openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
    clickOn(R.string.preferencesEditor)
    writeTo(R.id.inputKey, Preferences::osmTileScaleFactor.name)
    writeTo(R.id.inputValue, "0.5")
    clickDialogPositiveButton()

    assertContains(R.id.effectiveConfiguration, """"osmTileScaleFactor" : 0.5""")
  }

  @Test
  fun configuration_management_shows_an_error_when_putting_a_non_float_into_a_float() {
    launchActivity()
    openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
    clickOn(R.string.preferencesEditor)
    writeTo(R.id.inputKey, Preferences::osmTileScaleFactor.name)
    writeTo(R.id.inputValue, "not a float")
    clickDialogPositiveButton()
    assertContains(
        com.google.android.material.R.id.snackbar_text, R.string.preferencesEditorValueError)
  }

  @Test
  fun configuration_management_shows_an_error_when_setting_an_invalid_key() {
    launchActivity()
    openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
    clickOn(R.string.preferencesEditor)
    writeTo(R.id.inputKey, "Not a valid key")
    writeTo(R.id.inputValue, "not a float")
    clickDialogPositiveButton()
    assertContains(
        com.google.android.material.R.id.snackbar_text, R.string.preferencesEditorKeyError)
  }

  @Test
  fun editorActivityShowsDefaultConfig() {
    launchActivity()
    assertContains(R.id.effectiveConfiguration, """"_type" : "configuration"""")
  }

  @Test
  fun configuration_management_can_be_exported() {
    launchActivity()
    intending(anyIntent())
        .respondWith(Instrumentation.ActivityResult(0, Intent().apply { putExtra("foo", "bar") }))
    openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
    clickOn(R.string.exportConfiguration)
    intended(
        allOf(
            hasAction(Intent.ACTION_CREATE_DOCUMENT), hasExtra(Intent.EXTRA_TITLE, "config.otrc")))
  }
}
