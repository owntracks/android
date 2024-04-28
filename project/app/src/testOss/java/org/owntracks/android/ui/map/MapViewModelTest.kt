package org.owntracks.android.ui.map

import org.junit.Assert.assertTrue
import org.junit.Test

class MapViewModelTest {
  @Test
  fun `MapLayerStyle provider test asserts that same OSM style is the same`() {
    assertTrue(
        MapLayerStyle.OpenStreetMapNormal.isSameProviderAs(MapLayerStyle.OpenStreetMapNormal))
  }

  @Test
  fun `MapLayerStyle provider test asserts that same OSM style is the same as Wikimedia`() {
    assertTrue(
        MapLayerStyle.OpenStreetMapNormal.isSameProviderAs(MapLayerStyle.OpenStreetMapWikimedia))
  }
}
