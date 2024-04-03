package org.owntracks.android.ui.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapViewModelTest {
  @Test
  fun `MapLayerStyle provider test asserts that same Google Map style is the same`() {
    assertTrue(MapLayerStyle.GoogleMapDefault.isSameProviderAs(MapLayerStyle.GoogleMapDefault))
  }

  @Test
  fun `MapLayerStyle provider test asserts that same OSM style is the same`() {
    assertTrue(
        MapLayerStyle.OpenStreetMapNormal.isSameProviderAs(MapLayerStyle.OpenStreetMapNormal))
  }

  @Test
  fun `MapLayerStyle provider test asserts that different Google Map style is the same`() {
    assertTrue(MapLayerStyle.GoogleMapHybrid.isSameProviderAs(MapLayerStyle.GoogleMapSatellite))
  }

  @Test
  fun `MapLayerStyle provider test asserts that OSM is different to Google Map style`() {
    assertFalse(MapLayerStyle.GoogleMapHybrid.isSameProviderAs(MapLayerStyle.OpenStreetMapNormal))
  }
}
