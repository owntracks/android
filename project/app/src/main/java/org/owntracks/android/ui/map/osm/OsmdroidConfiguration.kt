package org.owntracks.android.ui.map.osm

import android.content.Context
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration

/** Sets osmdroid up, which needs doing before any osmdroid MapView is created */
fun configureOsmdroid(context: Context) {
  Configuration.getInstance().apply {
    load(context, PreferenceManager.getDefaultSharedPreferences(context))
    osmdroidBasePath.resolve("tiles").run {
      if (exists()) {
        deleteRecursively()
      }
    }
    osmdroidTileCache = context.noBackupFilesDir.resolve("osmdroid/tiles")
  }
}
