package org.owntracks.android.ui.map

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.support.ContactImageBindingAdapter
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
    private val contactImageBindingAdapter: ContactImageBindingAdapter
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return if (MapFragment::class.java.isAssignableFrom(classLoader.loadClass(className))) {
            OSMMapFragment(locationRepo, contactImageBindingAdapter)
        } else {
            super.instantiate(classLoader, className)
        }
    }
}