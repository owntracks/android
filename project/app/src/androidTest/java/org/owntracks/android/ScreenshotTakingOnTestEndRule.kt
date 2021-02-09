package org.owntracks.android

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

class ScreenshotTakingOnTestEndRule : TestWatcher() {
    override fun finished(description: Description?) {
        description?.run {
            takeScreenshot(parentFolderPath = "testscreenshots/${description.className}", screenShotName = description.methodName)
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
        storeFailureScreenshot(screenCapture.bitmap, screenShotName, parentFolderPath)
    }

    private fun storeFailureScreenshot(bitmap: Bitmap, screenshotName: String, parentFolderPath: String) {
        val contentResolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        // Check SDK version of device to determine how to save the screenshot.
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            useMediaStoreScreenshotStorage(
                    contentValues,
                    contentResolver,
                    screenshotName,
                    parentFolderPath,
                    bitmap
            )
        } else {
            usePublicExternalScreenshotStorage(
                    contentValues,
                    contentResolver,
                    screenshotName,
                    parentFolderPath,
                    bitmap
            )
        }
    }

    private fun useMediaStoreScreenshotStorage(
            contentValues: ContentValues,
            contentResolver: ContentResolver,
            screenshotName: String,
            parentFolderPath: String,
            bitmap: Bitmap
    ) {
        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, "$screenshotName.jpeg")
        contentValues.put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$parentFolderPath")
        contentValues.put(MediaStore.Downloads.IS_PENDING, 1)
        contentResolver.delete(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), "${MediaStore.Downloads.DISPLAY_NAME}=?", arrayOf("$screenshotName.jpeg"))
        val contentUri = contentResolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)
        if (contentUri == null) {
            Timber.e("ContentURI returned null for $screenshotName")
        }
        contentUri?.also {
            contentResolver.openOutputStream(it)?.let { outputStream -> saveScreenshotToStream(bitmap, outputStream) }
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(it, contentValues, null, null)
        }

    }

    private fun usePublicExternalScreenshotStorage(
            contentValues: ContentValues,
            contentResolver: ContentResolver,
            screenshotName: String,
            parentFolderPath: String,
            bitmap: Bitmap
    ) {
        @Suppress("DEPRECATION") val directory = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$parentFolderPath")

        if (!directory.exists()) {
            val createSuccess = directory.mkdirs()
            if (!createSuccess) {
                Timber.e("unable to create directory tree at ${directory}. Skipping screenshot saving.")
                return
            }

        }

        val file = File(directory, "$screenshotName.jpeg")
        saveScreenshotToStream(bitmap, FileOutputStream(file))

        val values = contentValues
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
    }

    private fun saveScreenshotToStream(bitmap: Bitmap, outputStream: OutputStream) {
        outputStream.use {
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, it)
            } catch (e: IOException) {
                Timber.e(e, "Screenshot not saved")
            }
        }
    }

}

