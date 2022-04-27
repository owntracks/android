package org.owntracks.android.ui.map

import androidx.databinding.ViewDataBinding
import org.owntracks.android.R
import org.owntracks.android.ui.map.osm.OSMMapFragment

enum class MapLayerStyle {
    GoogleMapDefault,
    GoogleMapHybrid,
    GoogleMapSatellite,
    GoogleMapTerrain,
    OpenStreetMapNormal,
    OpenStreetMapWikimedia;

    fun isSameProviderAs(mapLayerStyle: MapLayerStyle): Boolean {
        return setOf(
            "GoogleMap",
            "OpenStreetMap"
        ).any { name.startsWith(it) && mapLayerStyle.name.startsWith(it) }
    }

    fun getFragmentClass(): Class<out MapFragment<out ViewDataBinding>> {
        return when (this) {
            GoogleMapDefault, GoogleMapHybrid, GoogleMapSatellite, GoogleMapTerrain -> GoogleMapFragment::class.java
            OpenStreetMapNormal, OpenStreetMapWikimedia -> OSMMapFragment::class.java
        }
    }
}


val mapLayerStyleMenuItemsToStyles = mapOf(
    R.id.menuMapLayerGoogleMapDefault to MapLayerStyle.GoogleMapDefault,
    R.id.menuMapLayerGoogleMapHybrid to MapLayerStyle.GoogleMapHybrid,
    R.id.menuMapLayerGoogleMapSatellite to MapLayerStyle.GoogleMapSatellite,
    R.id.menuMapLayerGoogleMapTerrain to MapLayerStyle.GoogleMapTerrain,
    R.id.menuMapLayerOpenStreetMap to MapLayerStyle.OpenStreetMapNormal,
    R.id.menuMapLayerOpenStreetMapWikimedia to MapLayerStyle.OpenStreetMapWikimedia,
)