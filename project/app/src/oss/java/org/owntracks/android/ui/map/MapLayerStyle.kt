package org.owntracks.android.ui.map

import org.owntracks.android.R
import org.owntracks.android.preferences.types.FromConfiguration

enum class MapLayerStyle {
    OpenStreetMapNormal,
    OpenStreetMapWikimedia;

    @Suppress("UNUSED_PARAMETER")
    fun isSameProviderAs(_mapLayerStyle: MapLayerStyle): Boolean = true

    companion object {
        @JvmStatic
        @FromConfiguration
        fun getByValue(value: String): MapLayerStyle =
            MapLayerStyle.values()
                .firstOrNull { it.name == value } ?: OpenStreetMapNormal
    }
}

val mapLayerSelectorButtonsToStyles = mapOf(
    R.id.fabMapLayerOpenStreetMap to MapLayerStyle.OpenStreetMapNormal,
    R.id.fabMapLayerOpenStreetMapWikimedia to MapLayerStyle.OpenStreetMapWikimedia
)
