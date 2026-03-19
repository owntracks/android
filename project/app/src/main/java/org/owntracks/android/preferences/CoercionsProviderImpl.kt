package org.owntracks.android.preferences

import kotlin.reflect.KProperty

class CoercionsProviderImpl : CoercionsProvider {
  /**
   * Coerces a preference into an allowable range. Sometimes this coercion depends on the value of
   * other preferences so we need a way of getting those
   *
   * @param T the type of the preference
   * @param property the preference property
   * @param value the value of the preference that's being set
   * @param preferences an instance of [Preferences] that we can use to get other preference values
   * @return the coerced preference value
   */
  @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
  override fun <T> getCoercion(property: KProperty<*>, value: T, preferences: Preferences): T {
    return when (property) {
      Preferences::connectionTimeoutSeconds -> {
        (value as Int).coerceAtLeast(1)
      }
      Preferences::keepalive -> {
        (value as Int).coerceAtLeast(0)
      }
      Preferences::port -> {
        (value as Int).coerceAtLeast(1).coerceAtMost(65535)
      }
      Preferences::localNetworkPort -> {
        (value as Int).coerceAtLeast(1).coerceAtMost(65535)
      }
      else -> value
    }
        as T
  }
}
