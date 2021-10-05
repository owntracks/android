package org.owntracks.android.ui

import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickBack
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep

private const val sleepMillis = 1000L
fun clickOnAndWait(int: Int) {
    clickOn(int)
    sleep(sleepMillis)
}

fun clickOnAndWait(str: String) {
    clickOn(str)
    sleep(sleepMillis)
}

fun clickBackAndWait() {
    clickBack()
    sleep(sleepMillis)
}