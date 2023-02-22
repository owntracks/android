package org.owntracks.android.preferences

import kotlin.reflect.KMutableProperty

/**
 * Bundles together a [KMutableProperty] and a value, usually so that we can bundle together setter methods and the
 * correctly typed values into a collection, so that they can be applied later
 */
data class SetterAndValue<T>(val setter: KMutableProperty<T>, val value: T)
