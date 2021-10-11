package org.owntracks.android.ui

import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickBack
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import timber.log.Timber

private const val sleepMillis = 1000L
fun clickOnAndWait(int: Int) {
    clickOn(int)
    Timber.d("Sleeping for $sleepMillis ms")
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