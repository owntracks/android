package org.owntracks.android.ui.map.osm

import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration as OsmdroidConfiguration
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay

/** Sets osmdroid up, which needs doing before any osmdroid MapView is created */
fun configureOsmdroid(context: Context) {
  OsmdroidConfiguration.getInstance().apply {
    load(context, PreferenceManager.getDefaultSharedPreferences(context))
    osmdroidBasePath.resolve("tiles").run {
      if (exists()) {
        deleteRecursively()
      }
    }
    osmdroidTileCache = context.noBackupFilesDir.resolve("osmdroid/tiles")
  }
}

/** Inverts the map tiles in dark mode, as there aren't dark tiles */
fun MapView.setTilesForUiMode(configuration: Configuration) {
  overlayManager.tilesOverlay.setColorFilter(
      if (
          configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK) ==
              Configuration.UI_MODE_NIGHT_YES
      ) {
        TilesOverlay.INVERT_COLORS
      } else {
        null
      }
  )
}
