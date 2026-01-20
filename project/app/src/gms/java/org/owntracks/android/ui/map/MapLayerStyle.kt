package org.owntracks.android.ui.map

import com.google.android.gms.maps.GoogleMap
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

  /** Returns true if this layer style uses Google Maps, false for OpenStreetMap. */
  fun isGoogleMaps(): Boolean =
      when (this) {
        GoogleMapDefault,
        GoogleMapHybrid,
        GoogleMapSatellite,
        GoogleMapTerrain -> true
        OpenStreetMapNormal,
        OpenStreetMapWikimedia -> false
      }

  /** Returns the Google Maps map type constant for this layer style. */
  fun toGoogleMapType(): Int =
      when (this) {
        GoogleMapDefault -> GoogleMap.MAP_TYPE_NORMAL
        GoogleMapHybrid -> GoogleMap.MAP_TYPE_HYBRID
        GoogleMapSatellite -> GoogleMap.MAP_TYPE_SATELLITE
        GoogleMapTerrain -> GoogleMap.MAP_TYPE_TERRAIN
        else -> GoogleMap.MAP_TYPE_NORMAL
      }

  fun getFragmentClass(): Class<out MapFragment> {
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
