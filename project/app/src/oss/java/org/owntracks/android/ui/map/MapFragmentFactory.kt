package org.owntracks.android.ui.map

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import javax.inject.Inject
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.ui.map.osm.OSMMapFragment

/** An implementation of an [FragmentFactory] that always returns an [OSMMapFragment] */
class MapFragmentFactory
@Inject
constructor(
    private val preferences: Preferences,
) : FragmentFactory() {
  override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
    return if (MapFragment::class.java.isAssignableFrom(classLoader.loadClass(className))) {
      OSMMapFragment(preferences)
    } else {
      super.instantiate(classLoader, className)
    }
  }
}
