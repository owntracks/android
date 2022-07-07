package org.owntracks.android.ui.map

import androidx.databinding.ViewDataBinding
import org.owntracks.android.R
import org.owntracks.android.ui.map.osm.OSMMapFragment

enum class MapLayerStyle {
    OpenStreetMapNormal,
    OpenStreetMapWikimedia;

    @Suppress("UNUSED_PARAMETER")
    fun isSameProviderAs(_mapLayerStyle: MapLayerStyle): Boolean = true

    fun getFragmentClass(): Class<out MapFragment<out ViewDataBinding>> = OSMMapFragment::class.java
}

val mapLayerSelectorButtonsToStyles = mapOf(
    R.id.fabMapLayerOpenStreetMap to MapLayerStyle.OpenStreetMapNormal,
    R.id.fabMapLayerOpenStreetMapWikimedia to MapLayerStyle.OpenStreetMapWikimedia
)
