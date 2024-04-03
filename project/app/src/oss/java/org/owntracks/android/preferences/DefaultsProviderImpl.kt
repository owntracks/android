package org.owntracks.android.preferences

import kotlin.reflect.KProperty
import org.owntracks.android.preferences.types.ReverseGeocodeProvider
import org.owntracks.android.ui.map.MapLayerStyle

class DefaultsProviderImpl : DefaultsProvider {
  @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
  override fun <T> getDefaultValue(preferences: Preferences, property: KProperty<*>): T {
    return when (property) {
      Preferences::mapLayerStyle -> MapLayerStyle.OpenStreetMapNormal
      Preferences::reverseGeocodeProvider -> ReverseGeocodeProvider.NONE
      else -> super.getDefaultValue<T>(preferences, property)
    }
        as T
  }
}
