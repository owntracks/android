package org.owntracks.android.testutils.rules

import android.graphics.Bitmap
import android.util.Log
import androidx.test.core.app.takeScreenshotNoSync
import androidx.test.services.storage.TestStorage
import java.io.IOException
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class ScreenshotTakingOnTestEndRule : TestWatcher() {
    override fun failed(throwable: Throwable, description: Description) {
        description.run {
            takeScreenshot(screenShotName = "${description.className}/${description.methodName}")
        }
        super.finished(description)
    }

    private fun takeScreenshot(screenShotName: String) {
        Log.d("Screenshots", "Saving screenshot to '$screenShotName'")
        takeScreenshotNoSync().writeToTestStorage(screenShotName)
    }

    @Throws(IOException::class)
    fun Bitmap.writeToTestStorage(name: String) {
        writeToTestStorage(TestStorage(), name)
    }

    /**
     * Writes the contents of the [Bitmap] to a compressed png file to the given [TestStorage]
     *
     * @param testStorage the [TestStorage] to use
     * @param name a descriptive base name for the resulting file
     * @throws IOException if bitmap could not be compressed or written to storage
     */
    @Throws(IOException::class)
    fun Bitmap.writeToTestStorage(testStorage: TestStorage, name: String) {
        testStorage.openOutputFile("$name.png")
            .use {
                if (!this.compress(
                        Bitmap.CompressFormat.PNG,
                        /** PNG is lossless, so quality is ignored. */
                        0,
                        it
                    )
                ) {
                    throw IOException("Failed to compress bitmap")
                }
            }
    }
}
