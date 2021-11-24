package org.owntracks.android.ui.map

import android.graphics.Bitmap
import androidx.fragment.app.Fragment
import org.owntracks.android.location.LatLng

abstract class MapFragment : Fragment() {
    abstract fun clearMarkers()
    abstract fun updateCamera(latLng: LatLng)
    abstract fun updateMarker(id: String, latLng: LatLng)
    abstract fun removeMarker(id: String)
    abstract fun setMarkerImage(id: String, bitmap: Bitmap)
    abstract fun myLocationEnabled()
    abstract fun setMapLocationSource(mapLocationSource: MapLocationSource)
}