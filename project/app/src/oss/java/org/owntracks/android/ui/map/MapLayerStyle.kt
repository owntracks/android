package org.owntracks.android.ui.map

import org.owntracks.android.R
import org.owntracks.android.preferences.types.FromConfiguration

enum class MapLayerStyle {
  OpenStreetMapNormal,
  OpenStreetMapWikimedia;

  fun isSameProviderAs(@Suppress("UNUSED_PARAMETER") mapLayerStyle: MapLayerStyle): Boolean = true

  /** OSS flavor only has OpenStreetMap, so this always returns false. */
  fun isGoogleMaps(): Boolean = false

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): MapLayerStyle =
        MapLayerStyle.values().firstOrNull { it.name.equals(value, true) } ?: OpenStreetMapNormal
  }
}

val mapLayerSelectorButtonsToStyles =
    mapOf(
        R.id.fabMapLayerOpenStreetMap to MapLayerStyle.OpenStreetMapNormal,
        R.id.fabMapLayerOpenStreetMapWikimedia to MapLayerStyle.OpenStreetMapWikimedia)
