package org.owntracks.android.ui.map

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import dagger.hilt.android.qualifiers.ActivityContext
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.support.Preferences
import org.owntracks.android.ui.map.osm.OSMMapFragment
import javax.inject.Inject

/**
 * An implementation of an [FragmentFactory] that always returns an [OSMMapFragment]
 *
 * @property locationRepo required to create [MapFragment]
 * @property preferences can be used to decide what implementation of [MapFragment] should be used.
 */

class MapFragmentFactory @Inject constructor(
    private val locationRepo: LocationRepo,
    private val preferences: Preferences,
    @ActivityContext private val activityContext: Context,
    locationProviderClient: LocationProviderClient
) : FragmentFactory() {
    private val mapLocationSource = MapLocationSource(
        locationProviderClient,
        (activityContext as MapActivity).mapLocationSourceCallback
    )

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return if (MapFragment::class.java.isAssignableFrom(classLoader.loadClass(className))) {
            OSMMapFragment(locationRepo, mapLocationSource)
        } else {
            super.instantiate(classLoader, className)
        }
    }
}