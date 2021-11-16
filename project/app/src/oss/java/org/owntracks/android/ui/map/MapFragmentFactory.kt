package org.owntracks.android.ui.map

import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.support.Preferences
import org.owntracks.android.ui.map.osm.OSMMapFragment
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An implementation of an [IMapFragmentFactory] that always returns an [OSMMapFragment]
 *
 * @property locationRepo required to create [MapFragment]
 * @property preferences can be used to decide what implementation of [MapFragment] should be used.
 */
@Singleton
class MapFragmentFactory @Inject constructor(
    private val locationRepo: LocationRepo,
    private val preferences: Preferences
) :
    IMapFragmentFactory {
    override fun getMapFragment(
        mapFragment: MapFragment?,
        mapLocationSource: MapLocationSource
    ): MapFragment {
        return mapFragment
            ?: OSMMapFragment().apply {
                setMapLocationSource(mapLocationSource)
                locationRepo = this@MapFragmentFactory.locationRepo
            }
    }
}