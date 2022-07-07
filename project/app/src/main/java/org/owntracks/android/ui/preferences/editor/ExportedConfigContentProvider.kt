package org.owntracks.android.ui.preferences.editor

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.owntracks.android.data.repos.WaypointsRepo
import org.owntracks.android.support.Parser
import org.owntracks.android.support.Preferences
import timber.log.Timber
import java.io.FileOutputStream

class ExportedConfigContentProvider : ContentProvider() {
    private var exportedConfigJson: String = "{}"

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ExportedConfigContentProviderEntryPoint {
        fun preferences(): Preferences
        fun waypointsRepo(): WaypointsRepo
        fun parser(): Parser
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val appContext = context?.applicationContext ?: throw IllegalStateException()
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            ExportedConfigContentProviderEntryPoint::class.java
        )
        val preferences = hiltEntryPoint.preferences()
        val waypointsRepo = hiltEntryPoint.waypointsRepo()
        val parser = hiltEntryPoint.parser()

        val configurationMessage = preferences.exportToMessage()
        configurationMessage.waypoints = waypointsRepo.exportToMessage()
        exportedConfigJson = parser.toUnencryptedJsonPretty(configurationMessage)
        val matrixCursor =
            MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), 1)
        matrixCursor.addRow(arrayOf("config.otrc", exportedConfigJson.length.toLong()))
        return matrixCursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") {
            Timber.e("Can't export, mode set to $mode, expecting 'r'")
            // Cancel
            return null
        }

        return openPipeHelper(
            uri,
            "text/plain",
            null,
            exportedConfigJson.toByteArray()
        ) { parcelFileDescriptor, _, _, _, bytes ->
            try {
                FileOutputStream(parcelFileDescriptor.fileDescriptor).write(bytes)
            } catch (e: Exception) {
                Timber.e(e, "Can't write config to output")
            }
        }
    }

    override fun getType(uri: Uri): String = "text/plain"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
