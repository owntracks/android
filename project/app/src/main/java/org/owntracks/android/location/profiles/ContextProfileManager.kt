package org.owntracks.android.location.profiles

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.owntracks.android.di.ApplicationScope
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode
import timber.log.Timber

/**
 * Picks the [ContextProfile] that applies to the current [DeviceContext], and publishes the
 * resulting [LocatorSettings]. Anything that locates or reports should read [locatorSettings]
 * rather than the locator preferences directly.
 *
 * Changing one of the locator preferences while a profile is active (e.g. the user picking a
 * monitoring mode) suspends that profile until the context changes and a different one matches.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ContextProfileManager
@Inject
constructor(
    private val preferences: Preferences,
    deviceContextProvider: DeviceContextProvider,
    @ApplicationScope scope: CoroutineScope,
) : Preferences.OnPreferenceChangeListener {
  private var deviceContext = DeviceContext()
  /** No profile is matched until the device context is known, which it isn't to begin with */
  private var deviceContextKnown = false
  // Restored, so that a manual change still wins after the app has been restarted
  private var selection =
      ProfileSelection(
          suspendedProfileId = preferences.suspendedContextProfileId.takeIf { it.isNotEmpty() }
      )

  private val enabled = MutableStateFlow(preferences.contextProfilesEnabled)

  private val mutableReady = MutableStateFlow(!enabled.value)
  /**
   * Whether the profile to apply is known yet. Profiles are matched once the device context is
   * known, so until then nothing should be located with [locatorSettings], which may change.
   */
  val ready: StateFlow<Boolean> = mutableReady

  private val mutableMatchedProfile = MutableStateFlow<ContextProfile?>(null)
  /**
   * The first profile whose conditions match, whether or not it's being applied. It isn't applied
   * while the user has manually overridden it.
   */
  val matchedProfile: StateFlow<ContextProfile?> = mutableMatchedProfile

  private val mutableActiveProfile = MutableStateFlow<ContextProfile?>(null)
  /** The profile currently being applied, if any */
  val activeProfile: StateFlow<ContextProfile?> = mutableActiveProfile

  private val mutableLocatorSettings = MutableStateFlow(LocatorSettings.from(preferences, null))
  val locatorSettings: StateFlow<LocatorSettings> = mutableLocatorSettings

  init {
    preferences.registerOnPreferenceChangedListener(this)
    update()
    scope.launch {
      // Only watch the device while profiles are on
      enabled
          .flatMapLatest { enabled ->
            if (enabled) deviceContextProvider.deviceContext else flowOf(DeviceContext())
          }
          .collect {
            Timber.d("Device context changed: $it")
            synchronized(this@ContextProfileManager) {
              deviceContext = it
              deviceContextKnown = true
            }
            // Settled before saying it's ready, so that nothing acts on the unmatched settings
            update()
            mutableReady.value = true
          }
    }
  }

  /**
   * Sets the monitoring mode as chosen by the user. This wins over any active profile, even if the
   * preference already had this value.
   */
  fun setMonitoringModeManually(monitoringMode: MonitoringMode) {
    synchronized(this) { selection = selection.withManualOverride() }
    preferences.monitoring = monitoringMode
    update()
  }

  override fun onPreferenceChanged(properties: Set<String>) {
    if (Preferences::contextProfilesEnabled.name in properties) {
      // Not ready again until the device has been looked at
      if (preferences.contextProfilesEnabled) {
        synchronized(this) { deviceContextKnown = false }
        mutableReady.value = false
      }
      enabled.value = preferences.contextProfilesEnabled
    }
    if (properties.intersect(PROFILE_PREFERENCES + LOCATOR_PREFERENCES).isEmpty()) return
    // Changing a setting the active profile overrides means the user wants that instead
    synchronized(this) {
      selection.effective?.let { active ->
        if (properties.intersect(active.overrides.overriddenPreferences()).isNotEmpty()) {
          Timber.i("Preferences that ${active.name} overrides changed, suspending it")
          selection = selection.withManualOverride()
        }
      }
    }
    update()
  }

  private fun update() =
      synchronized(this) {
        val profiles =
            if (preferences.contextProfilesEnabled && deviceContextKnown) {
              decodeContextProfiles(preferences.contextProfiles)
            } else {
              emptyList()
            }
        // Until the device is known, nothing is matched, but any suspension is kept for when it is
        if (deviceContextKnown)
            selection = selection.withMatched(profiles.firstMatching(deviceContext))
        (selection.suspendedProfileId ?: "").let {
          if (it != preferences.suspendedContextProfileId)
              preferences.suspendedContextProfileId = it
        }
        val effective = selection.effective.takeIf { deviceContextKnown }
        if (effective?.id != mutableActiveProfile.value?.id) {
          Timber.i("Active context profile is now ${effective?.name ?: "none"}")
        }
        mutableMatchedProfile.value = selection.matched.takeIf { deviceContextKnown }
        mutableActiveProfile.value = effective
        mutableLocatorSettings.value = LocatorSettings.from(preferences, effective?.overrides)
      }

  companion object {
    private val PROFILE_PREFERENCES =
        setOf(Preferences::contextProfiles.name, Preferences::contextProfilesEnabled.name)

    /** The preferences that [LocatorOverrides] can override */
    private val LOCATOR_PREFERENCES =
        setOf(
            Preferences::monitoring.name,
            Preferences::locatorInterval.name,
            Preferences::moveModeLocatorInterval.name,
            Preferences::locatorDisplacement.name,
            Preferences::locatorPriority.name,
            Preferences::ping.name,
        )
  }
}
