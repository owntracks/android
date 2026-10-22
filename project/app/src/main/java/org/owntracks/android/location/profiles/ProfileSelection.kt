package org.owntracks.android.location.profiles

/**
 * Tracks which profile currently matches, and whether the user has manually overridden it. A manual
 * override suspends the [matched] profile until a different profile (or none) matches.
 */
data class ProfileSelection(
    val matched: ContextProfile? = null,
    val suspendedProfileId: String? = null,
) {
  val effective: ContextProfile?
    get() = matched?.takeUnless { it.id == suspendedProfileId }

  /**
   * A suspension lasts while its profile keeps matching, so is dropped when a different one does
   */
  fun withMatched(matched: ContextProfile?): ProfileSelection =
      ProfileSelection(matched, suspendedProfileId?.takeIf { it == matched?.id })

  fun withManualOverride(): ProfileSelection = copy(suspendedProfileId = matched?.id)
}
