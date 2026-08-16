package org.owntracks.android.ui.map

import kotlinx.serialization.Serializable
import org.owntracks.android.R
import org.owntracks.android.preferences.types.FromConfiguration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex

@Serializable
enum class MapLayerStyle {
  OpenStreetMapNormal,
  OpenStreetMapWikimedia,
  AmapVector,
  AmapSatellite;

  fun isSameProviderAs(mapLayerStyle: MapLayerStyle): Boolean {
    return providerGroup() == mapLayerStyle.providerGroup()
  }

  private fun providerGroup(): String =
      when (this) {
        OpenStreetMapNormal,
        OpenStreetMapWikimedia -> "OpenStreetMap"
        AmapVector,
        AmapSatellite -> "Amap"
      }

  fun getTileSource(): ITileSource =
      when (this) {
        OpenStreetMapNormal -> TileSourceFactory.MAPNIK
        OpenStreetMapWikimedia -> TileSourceFactory.WIKIMEDIA
        AmapVector -> AMAP_VECTOR
        AmapSatellite -> AMAP_SATELLITE
      }

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): MapLayerStyle =
        entries.firstOrNull { it.name.equals(value, true) } ?: OpenStreetMapNormal
  }
}

/**
 * Amap (高德) vector tiles served over HTTPS without an API key. The URL pattern uses query
 * string parameters rather than the standard `{z}/{x}/{y}.png` slippy map convention, so we
 * subclass XYTileSource to override the URL builder.
 */
private val AMAP_VECTOR: ITileSource =
    object :
        XYTileSource(
            "AmapVector",
            1,
            18,
            256,
            ".png",
            arrayOf(
                "https://webrd01.is.autonavi.com/",
                "https://webrd02.is.autonavi.com/",
                "https://webrd03.is.autonavi.com/",
                "https://webrd04.is.autonavi.com/")) {
      override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "${getBaseUrl()}appmaptile?lang=zh_cn&size=1&scale=1&style=8&x=$x&y=$y&z=$z"
      }

      override fun getCopyrightNotice(): String = "© Amap.com"
    }

/**
 * Amap satellite imagery tiles (no API key required). Uses the `webst0X` subdomain (different
 * from the vector layer's `webrd0X`) and the `style=6` parameter for raster imagery.
 */
private val AMAP_SATELLITE: ITileSource =
    object :
        XYTileSource(
            "AmapSatellite",
            1,
            18,
            256,
            ".jpg",
            arrayOf(
                "https://webst01.is.autonavi.com/",
                "https://webst02.is.autonavi.com/",
                "https://webst03.is.autonavi.com/",
                "https://webst04.is.autonavi.com/")) {
      override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "${getBaseUrl()}appmaptile?style=6&x=$x&y=$y&z=$z"
      }

      override fun getCopyrightNotice(): String = "© Amap.com"
    }

val mapLayerSelectorButtonsToStyles =
    mapOf(
        R.id.fabMapLayerOpenStreetMap to MapLayerStyle.OpenStreetMapNormal,
        R.id.fabMapLayerOpenStreetMapWikimedia to MapLayerStyle.OpenStreetMapWikimedia,
        R.id.fabMapLayerAmapVector to MapLayerStyle.AmapVector,
        R.id.fabMapLayerAmapSatellite to MapLayerStyle.AmapSatellite)