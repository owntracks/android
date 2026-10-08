package org.owntracks.android.location.profiles

import org.owntracks.android.preferences.Preferences

/** The stored context profiles, decoded */
var Preferences.contextProfileList: List<ContextProfile>
  get() = decodeContextProfiles(contextProfiles)
  set(value) {
    contextProfiles = encodeContextProfiles(value, keepingUndecodableFrom = contextProfiles)
  }

/** Replaces the profile with the same id, or adds it to the end if there isn't one */
fun List<ContextProfile>.replacing(profile: ContextProfile): List<ContextProfile> =
    if (any { it.id == profile.id }) {
      map { if (it.id == profile.id) profile else it }
    } else {
      this + profile
    }

fun List<ContextProfile>.without(id: String): List<ContextProfile> = filterNot { it.id == id }

/** Moves the profile [offset] places later in the list (earlier if negative), within its bounds */
fun List<ContextProfile>.moving(id: String, offset: Int): List<ContextProfile> {
  val index = indexOfFirst { it.id == id }
  if (index < 0) return this
  return toMutableList().apply { add((index + offset).coerceIn(0, lastIndex), removeAt(index)) }
}
