package org.owntracks.android

import android.util.Log
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.runner.screenshot.Screenshot
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.*

class ScreenshotTakingOnTestEndRule : TestWatcher() {
    override fun finished(description: Description?) {
        description?.run {
            takeScreenshot(screenShotName = "${description.className}/${description.methodName}")
        } ?: run {
            val uuid = UUID.randomUUID()
            println("Test finished but no description provided. Capturing under $uuid")
            takeScreenshot(screenShotName = uuid.toString())
        }
        super.finished(description)
    }

    private fun takeScreenshot(screenShotName: String) {
        Log.d("Screenshots", "Saving screenshot to '$screenShotName'")
        Screenshot.capture().bitmap.writeToTestStorage(screenShotName)
    }
}

