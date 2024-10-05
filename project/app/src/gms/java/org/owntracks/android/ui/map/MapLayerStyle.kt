package org.owntracks.android.ui.map

import androidx.databinding.ViewDataBinding
import org.owntracks.android.R
import org.owntracks.android.preferences.types.FromConfiguration
import org.owntracks.android.ui.map.osm.OSMMapFragment

enum class MapLayerStyle {
  GoogleMapDefault,
  GoogleMapHybrid,
  GoogleMapSatellite,
  GoogleMapTerrain,
  OpenStreetMapNormal,
  OpenStreetMapWikimedia;

  fun isSameProviderAs(mapLayerStyle: MapLayerStyle): Boolean {
    return setOf("GoogleMap", "OpenStreetMap").any {
      name.startsWith(it) && mapLayerStyle.name.startsWith(it)
    }
  }

  fun getFragmentClass(): Class<out MapFragment<out ViewDataBinding>> {
    return when (this) {
      GoogleMapDefault,
      GoogleMapHybrid,
      GoogleMapSatellite,
      GoogleMapTerrain -> GoogleMapFragment::class.java
      OpenStreetMapNormal,
      OpenStreetMapWikimedia -> OSMMapFragment::class.java
    }
  }

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): MapLayerStyle =
        entries.firstOrNull { it.name.equals(value, true) } ?: GoogleMapDefault
  }
}

val mapLayerSelectorButtonsToStyles =
    mapOf(
        R.id.fabMapLayerGoogleNormal to MapLayerStyle.GoogleMapDefault,
        R.id.fabMapLayerGoogleHybrid to MapLayerStyle.GoogleMapHybrid,
        R.id.fabMapLayerGoogleTerrain to MapLayerStyle.GoogleMapTerrain,
        R.id.fabMapLayerOpenStreetMap to MapLayerStyle.OpenStreetMapNormal,
        R.id.fabMapLayerOpenStreetMapWikimedia to MapLayerStyle.OpenStreetMapWikimedia)
