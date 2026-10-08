package org.owntracks.android.ui.preferences

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.location.profiles.ContextProfile
import org.owntracks.android.location.profiles.ContextProfileManager
import org.owntracks.android.location.profiles.LocatorOverrides
import org.owntracks.android.location.profiles.contextProfileList
import org.owntracks.android.location.profiles.moving
import org.owntracks.android.preferences.Preferences

@AndroidEntryPoint
class ContextProfilesFragment @Inject constructor() :
    AbstractPreferenceFragment(), Preferences.OnPreferenceChangeListener {
  @Inject lateinit var waypointsRepo: WaypointsRepo

  @Inject lateinit var contextProfileManager: ContextProfileManager

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    setPreferencesFromResource(R.xml.preferences_context_profiles, rootKey)
    findPreference<Preference>("addProfile")?.setOnPreferenceClickListener {
      val profile =
          ContextProfile(
              UUID.randomUUID().toString(),
              getString(R.string.preferencesContextProfileNewName),
              emptyList(),
              LocatorOverrides(),
          )
      preferences.contextProfileList += profile
      it.extras.putString(ContextProfileFragment.PROFILE_ID, profile.id)
      // Not handled, so that the preference goes on to open the new profile
      false
    }
  }

  private var waypointNames: Map<Long, String> = emptyMap()

  override fun onResume() {
    super.onResume()
    preferences.registerOnPreferenceChangedListener(this)
  }

  override fun onPause() {
    super.onPause()
    preferences.unregisterOnPreferenceChangedListener(this)
  }

  override fun onPreferenceChanged(properties: Set<String>) {
    if (Preferences::contextProfilesEnabled.name in properties && view != null) {
      viewLifecycleOwner.lifecycleScope.launch { showProfiles() }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    // Follow which profile applies as it changes, e.g. connecting to Wi-Fi while looking at this
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        waypointNames = waypointsRepo.getAll().associate { it.tst.epochSecond to it.description }
        combine(contextProfileManager.matchedProfile, contextProfileManager.activeProfile) {
                matched,
                active ->
              matched to active
            }
            .collect { showProfiles() }
      }
    }
  }

  private fun showProfiles() {
    val category = findPreference<PreferenceCategory>("profiles") ?: return
    category.removeAll()
    val status =
        profileStatus(
            preferences.contextProfilesEnabled,
            contextProfileManager.matchedProfile.value,
            contextProfileManager.activeProfile.value,
        )
    findPreference<Preference>("status")?.summary =
        when (status) {
          ProfileStatus.Off -> getString(R.string.contextProfileStatusOff)
          ProfileStatus.NoneApplies -> getString(R.string.contextProfileStatusNoneApplies)
          is ProfileStatus.Active -> status.profile.name
          is ProfileStatus.Paused ->
              getString(R.string.contextProfileStatusPaused, status.profile.name)
        }
    val activeId = (status as? ProfileStatus.Active)?.profile?.id
    val profiles = preferences.contextProfileList
    profiles.forEachIndexed { index, profile ->
      category.addPreference(
          ReorderablePreference(requireContext(), index > 0, index < profiles.lastIndex) { offset ->
                preferences.contextProfileList =
                    preferences.contextProfileList.moving(profile.id, offset)
                showProfiles()
              }
              .apply {
                key = "profile-${profile.id}"
                title = profile.name
                summary =
                    requireContext().describeContextProfile(profile, waypointNames).let {
                      if (profile.id == activeId) {
                        getString(R.string.contextProfileSummaryActive, it)
                      } else {
                        it
                      }
                    }
                isIconSpaceReserved = false
                fragment = ContextProfileFragment::class.java.name
                extras.putString(ContextProfileFragment.PROFILE_ID, profile.id)
              }
      )
    }
  }
}
