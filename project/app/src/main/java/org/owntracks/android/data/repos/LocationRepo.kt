package org.owntracks.android.data.repos

import android.location.Location
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.owntracks.android.location.LatLng
import org.owntracks.android.ui.map.MapLocationZoomLevelAndRotation
import org.owntracks.android.ui.map.MapViewModel

@Singleton
class LocationRepo @Inject constructor() {
    /**
     * The last location that was published to the network
     */
    val currentPublishedLocation: StateFlow<Location?>
        get() = mutableCurrentPublishedLocation
    private val mutableCurrentPublishedLocation = MutableStateFlow<Location?>(null)

    val currentLocationTime: Long
        get() = currentPublishedLocation.value?.time ?: 0

    suspend fun setCurrentPublishedLocation(location: Location) {
        mutableCurrentPublishedLocation.emit(location)
    }

    var currentBlueDotOnMapLocation: LatLng? = null

    /**
     * Where the map was last moved to. This might have been from an explicit user action, or from
     * the map being moved due to being in DEVICE or CONTACT modes
     */
    var mapViewWindowLocationAndZoom: MapLocationZoomLevelAndRotation? = null

    /**
     * The view mode of the map
     */
    var viewMode: MapViewModel.ViewMode = MapViewModel.ViewMode.Device
}
