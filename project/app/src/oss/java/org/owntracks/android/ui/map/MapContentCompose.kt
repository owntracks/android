package org.owntracks.android.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.ContactImageBindingAdapter

/**
 * Returns true if Compose-based maps should be used for the given map layer style. In OSS flavor,
 * this always returns false as we use AndroidView with OSMMapFragment.
 */
fun shouldUseComposeMaps(mapLayerStyle: MapLayerStyle?): Boolean = false

/**
 * Compose-based map content for OSS flavor. This is a no-op since OSS uses AndroidView with
 * OSMMapFragment instead of Compose-based maps.
 */
@Composable
fun MapContentCompose(
    viewModel: MapViewModel,
    contactImageBindingAdapter: ContactImageBindingAdapter,
    preferences: Preferences,
    modifier: Modifier = Modifier
) {
  // OSS flavor uses AndroidView with OSMMapFragment, so this composable is not used.
  // It exists to maintain API compatibility with GMS flavor.
}
