package org.owntracks.android.ui.map

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import javax.inject.Inject
import org.owntracks.android.preferences.Preferences
import timber.log.Timber

/**
 * An [FragmentFactory] instance that can either provide a GoogleMap fragment or an OSM fragment
 * depending on whether the use-osm-map preference is set.
 *
 * @property preferences can be used to decide what implementation of [MapFragment] should be used.
 * @property contactImageBindingAdapter A binding adapter that can render contact images in views
 */
class MapFragmentFactory
@Inject
constructor(
    private val preferences: Preferences,
) : FragmentFactory() {
  override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
    Timber.d("Instantiating Fragment for $className")
    return if (MapFragment::class.java.isAssignableFrom(classLoader.loadClass(className))) {
      preferences.mapLayerStyle
          .getFragmentClass()
          .getConstructor(Preferences::class.java)
          .newInstance(preferences)
    } else {
      super.instantiate(classLoader, className)
    }
  }
}
