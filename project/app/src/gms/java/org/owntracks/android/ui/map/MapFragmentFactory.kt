package org.owntracks.android.ui.map

import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.support.Preferences
import org.owntracks.android.ui.map.osm.OSMMapFragment
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An [IMapFragmentFactory] instance that can either provide a GoogleMap fragment or an OSM fragment
 * depending on whether the use-osm-map preference is set.
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
        return if (mapFragment != null &&
            (
                    (
                            mapFragment is GoogleMapFragment &&
                                    !preferences.experimentalFeatures.contains(
                                        Preferences.EXPERIMENTAL_FEATURE_USE_OSM_MAP
                                    ))
                            ||
                            (mapFragment is OSMMapFragment &&
                                    preferences.experimentalFeatures.contains(
                                        Preferences.EXPERIMENTAL_FEATURE_USE_OSM_MAP
                                    ))
                    )
        ) {
            mapFragment
        } else {
            if (preferences.experimentalFeatures.contains(Preferences.EXPERIMENTAL_FEATURE_USE_OSM_MAP)) {
                OSMMapFragment().apply {
                    setMapLocationSource(mapLocationSource)
                    locationRepo = this@MapFragmentFactory.locationRepo
                }
            } else {
                GoogleMapFragment().apply {
                    setMapLocationSource(mapLocationSource)
                    locationRepo = this@MapFragmentFactory.locationRepo
                }
            }
        }
    }
}