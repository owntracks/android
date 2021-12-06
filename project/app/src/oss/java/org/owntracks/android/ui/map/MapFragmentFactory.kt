package org.owntracks.android.ui.map

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.support.Preferences
import org.owntracks.android.ui.map.osm.OSMMapFragment
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An implementation of an [FragmentFactory] that always returns an [OSMMapFragment]
 *
 * @property locationRepo required to create [MapFragment]
 * @property preferences can be used to decide what implementation of [MapFragment] should be used.
 */
@Singleton
class MapFragmentFactory @Inject constructor(
    private val locationRepo: LocationRepo,
    private val preferences: Preferences
) : FragmentFactory() {
    var mapLocationSource: MapLocationSource? = null
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return if (MapFragment::class.java.isAssignableFrom(classLoader.loadClass(className))) {
            OSMMapFragment(locationRepo, mapLocationSource!!)
        } else {
            super.instantiate(classLoader, className)
        }
    }
}