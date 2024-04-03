package org.owntracks.android.ui.status.logs

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import java.io.FileOutputStream
import org.owntracks.android.logging.TimberInMemoryLogTree
import timber.log.Timber

class ExportedLogContentProvider : ContentProvider() {

  private fun logForUri(uri: Uri): ByteArray? =
      Timber.forest()
          .filterIsInstance(TimberInMemoryLogTree::class.java)
          .firstOrNull()
          ?.logLines()
          ?.filter {
            if (uri.pathSegments.contains("debug=true")) {
              it.priority >= Log.DEBUG
            } else {
              it.priority >= Log.INFO
            }
          }
          ?.joinToString("\n") { it.toExportedString() }
          ?.toByteArray()

  override fun insert(uri: Uri, values: ContentValues?): Uri? {
    return null
  }

  override fun query(
      uri: Uri,
      projection: Array<out String>?,
      selection: String?,
      selectionArgs: Array<out String>?,
      sortOrder: String?
  ): Cursor? =
      logForUri(uri)?.let {
        val m = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), 1)
        m.addRow(arrayOf("owntracks-log.txt", it.size.toLong()))
        m
      }

  override fun onCreate(): Boolean = true

  override fun update(
      uri: Uri,
      values: ContentValues?,
      selection: String?,
      selectionArgs: Array<out String>?
  ): Int {
    return 0
  }

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
    return 0
  }

  override fun getType(uri: Uri): String = "text/plain"

  override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
    if (mode != "r") return null
    val log = logForUri(uri) ?: return null
    return openPipeHelper(uri, "text/plain", null, log) { output, _, _, _, l ->
      try {
        FileOutputStream(output.fileDescriptor).write(l!!)
      } catch (e: Exception) {
        Timber.e(e, "Can't write logs to output")
      }
    }
  }
}
