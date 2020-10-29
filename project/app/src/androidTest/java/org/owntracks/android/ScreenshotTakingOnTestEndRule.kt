package org.owntracks.android

import android.util.Log
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.Screenshot
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import java.io.IOException
import java.util.*

class ScreenshotTakingOnTestEndRule : TestWatcher() {
    override fun finished(description: Description?) {
        description?.run {
            takeScreenshot(parentFolderPath = description.className, screenShotName = description.methodName)
        } ?: run {
            val uuid = UUID.randomUUID()
            println("Test finished but no description provided. Capturing under $uuid")
            takeScreenshot(parentFolderPath = uuid.toString(), screenShotName = uuid.toString())
        }
        super.finished(description)
    }

    private fun takeScreenshot(parentFolderPath: String = "", screenShotName: String) {
        Log.d("Screenshots", "Taking screenshot of '$screenShotName'")
        val screenCapture = Screenshot.capture()
        val processors = setOf(CustomScreenCaptureProcessor(parentFolderPath))
        try {
            screenCapture.apply {
                name = screenShotName
                process(processors)
            }
            Log.d("Screenshots", "Screenshot taken")
        } catch (ex: IOException) {
            Log.e("Screenshots", "Could not take the screenshot", ex)
        }
    }

    class CustomScreenCaptureProcessor(parentFolderPath: String) : BasicScreenCaptureProcessor() {
        init {
            this.mDefaultScreenshotPath = File(
                    "/storage/emulated/0/Pictures",
                    "screenshots/$parentFolderPath"
            )
        }

        override fun getFilename(prefix: String): String = prefix
    }
}

