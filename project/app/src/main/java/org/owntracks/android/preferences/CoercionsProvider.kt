package org.owntracks.android.preferences

import kotlin.reflect.KProperty

interface CoercionsProvider {
  fun <T> getCoercion(property: KProperty<*>, value: T, preferences: Preferences): T
}
