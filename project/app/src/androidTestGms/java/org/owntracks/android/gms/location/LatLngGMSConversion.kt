package org.owntracks.android.gms.location

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.location.LatLng

@RunWith(AndroidJUnit4::class)
@SmallTest
class LatLngGMSConversion {

  @Test
  fun canConvertLatLngToGMS() {
    val lat = 51.5732
    val long = 0.5763
    val latLng = LatLng(lat, long)
    val gmsLatLng = latLng.toGMSLatLng()
    assertEquals(lat, gmsLatLng.latitude, 0.0001)
    assertEquals(long, gmsLatLng.longitude, 0.0001)
  }
}
