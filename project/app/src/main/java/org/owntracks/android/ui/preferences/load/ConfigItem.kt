package org.owntracks.android.ui.preferences.load

import androidx.annotation.StringRes

sealed class ConfigItem {
  data class Header(@param:StringRes val labelRes: Int) : ConfigItem()

  /**
   * A preference key/value pair that is changing. [oldValue] is the current preference value, or
   * null if the key is not a known preference. [newValue] is the incoming value from the config
   * being loaded.
   */
  data class KeyValue(
    val key: String,
    val newValue: String,
    val oldValue: String? = null,
    @param:StringRes val labelRes: Int? = null,
  ) : ConfigItem()

  data class Waypoint(
      val description: String,
      val latitude: Double,
      val longitude: Double,
      val radius: Int?
  ) : ConfigItem()

  /** Footer row summarising how many settings are already at the incoming value. */
  data class Summary(val count: Int) : ConfigItem()
}
