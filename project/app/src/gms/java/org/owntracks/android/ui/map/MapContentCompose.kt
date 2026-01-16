package org.owntracks.android.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.ContactImageBindingAdapter

/**
 * Returns true if Compose-based maps should be used for the given map layer style. In GMS flavor,
 * this returns true for Google Maps styles.
 */
fun shouldUseComposeMaps(mapLayerStyle: MapLayerStyle?): Boolean = mapLayerStyle?.isGoogleMaps() ?: true

/**
 * Compose-based map content for GMS flavor. Renders GoogleMapContent for Google Maps styles.
 */
@Composable
fun MapContentCompose(
    viewModel: MapViewModel,
    contactImageBindingAdapter: ContactImageBindingAdapter,
    preferences: Preferences,
    modifier: Modifier = Modifier
) {
  GoogleMapContent(
      viewModel = viewModel,
      contactImageBindingAdapter = contactImageBindingAdapter,
      preferences = preferences,
      modifier = modifier)
}
