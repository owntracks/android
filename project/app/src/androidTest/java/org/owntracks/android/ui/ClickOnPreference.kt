package org.owntracks.android.ui

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickBack
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import org.hamcrest.Matchers.allOf

private const val SLEEP_MILLIS = 100L

fun clickOnDrawerAndWait(text: Int) {
  Espresso.onView(
          allOf(withId(com.mikepenz.materialdrawer.R.id.material_drawer_name), withText(text)))
      .perform(click())
  sleep(SLEEP_MILLIS)
}

fun clickOnAndWait(int: Int) {
  clickOn(int)
  sleep(SLEEP_MILLIS)
}

fun clickOnAndWait(str: String) {
  clickOn(str)
  sleep(SLEEP_MILLIS)
}

fun clickBackAndWait() {
  clickBack()
  sleep(SLEEP_MILLIS)
}
