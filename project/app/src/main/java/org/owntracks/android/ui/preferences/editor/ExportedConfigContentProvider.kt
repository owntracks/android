package org.owntracks.android.ui.preferences.editor

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.widget.Toast
import dagger.android.AndroidInjection
import org.owntracks.android.R
import org.owntracks.android.data.repos.WaypointsRepo
import org.owntracks.android.support.Parser
import org.owntracks.android.support.Preferences
import timber.log.Timber
import java.io.FileOutputStream
import javax.inject.Inject

class ExportedConfigContentProvider : ContentProvider() {
    private var exportedConfigJson: String = "{}"

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var waypointsRepo: WaypointsRepo

    @Inject
    lateinit var parser: Parser

    override fun onCreate(): Boolean {
        AndroidInjection.inject(this)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val configurationMessage = preferences.exportToMessage()
        configurationMessage.waypoints = waypointsRepo.exportToMessage()
        exportedConfigJson = parser.toJsonPlainPretty(configurationMessage)
        val matrixCursor =
            MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), 1)
        matrixCursor.addRow(arrayOf("config.otrc", exportedConfigJson.length.toLong()))
        return matrixCursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") {
            // We have no view to attach snackbars to, so have to use a toast here
            Toast.makeText(context, R.string.preferencesExportFailed, Toast.LENGTH_SHORT).show()
            return null
        }

        return openPipeHelper(
            uri,
            "text/plain",
            null,
            exportedConfigJson.toByteArray()
        ) { output, _, _, _, l ->
            try {
                FileOutputStream(output.fileDescriptor).write(l)
            } catch (e: Exception) {
                Timber.e(e, "Can't write config to output")
                Toast.makeText(context, R.string.preferencesExportFailed, Toast.LENGTH_SHORT).show()
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