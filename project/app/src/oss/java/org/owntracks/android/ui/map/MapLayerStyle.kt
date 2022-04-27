package org.owntracks.android.ui.map

import androidx.databinding.ViewDataBinding
import org.owntracks.android.R
import org.owntracks.android.ui.map.osm.OSMMapFragment

enum class MapLayerStyle {
    OpenStreetMapNormal,
    OpenStreetMapWikimedia;

    fun isSameProviderAs(mapLayerStyle: MapLayerStyle): Boolean {
        return setOf(
            "GoogleMap",
            "OpenStreetMap"
        ).any { name.startsWith(it) && mapLayerStyle.name.startsWith(it) }
    }

    fun getFragmentClass(): Class<out MapFragment<out ViewDataBinding>> = OSMMapFragment::class.java
}


val mapLayerStyleMenuItemsToStyles = mapOf(
    R.id.menuMapLayerOpenStreetMap to MapLayerStyle.OpenStreetMapNormal,
    R.id.menuMapLayerOpenStreetMapWikimedia to MapLayerStyle.OpenStreetMapWikimedia,
)