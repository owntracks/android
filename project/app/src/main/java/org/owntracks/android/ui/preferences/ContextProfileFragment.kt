package org.owntracks.android.ui.preferences

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToLong
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.location.LocatorPriority
import org.owntracks.android.location.profiles.ChargingSource
import org.owntracks.android.location.profiles.Condition
import org.owntracks.android.location.profiles.ConditionMatch
import org.owntracks.android.location.profiles.ConnectedBluetoothDevices
import org.owntracks.android.location.profiles.ContextProfile
import org.owntracks.android.location.profiles.LocatorOverrides
import org.owntracks.android.location.profiles.StaticLocation
import org.owntracks.android.location.profiles.contextProfileList
import org.owntracks.android.location.profiles.replacing
import org.owntracks.android.location.profiles.without
import org.owntracks.android.net.WifiInfoProvider
import org.owntracks.android.net.takeIfKnownSSID
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.ui.map.picker.LocationPickerActivity

/** Edits the [ContextProfile] given by the [PROFILE_ID] argument */
@AndroidEntryPoint
class ContextProfileFragment @Inject constructor() : AbstractPreferenceFragment() {
  @Inject lateinit var wifiInfoProvider: WifiInfoProvider

  @Inject lateinit var connectedBluetoothDevices: ConnectedBluetoothDevices

  @Inject lateinit var waypointsRepo: WaypointsRepo

  private val profileId by lazy { requireArguments().getString(PROFILE_ID)!! }

  private val profile: ContextProfile?
    get() = preferences.contextProfileList.firstOrNull { it.id == profileId }

  private var waypointNames: Map<Long, String> = emptyMap()

  private val pickLocation =
      registerForActivityResult(LocationPickerActivity.PickLocation()) { result ->
        when (result) {
          is LocationPickerActivity.Result.Picked ->
              result.latLng.run { setStaticLocation(latitude.value, longitude.value) }
          LocationPickerActivity.Result.Cleared ->
              updateOverrides { it.copy(staticLocation = null) }
          null -> {}
        }
      }

  private val bluetoothPermissionRequest =
      registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
          // Devices that were already connected weren't visible without the permission
          connectedBluetoothDevices.refresh()
          chooseBluetoothDevice()
        } else {
          Toast.makeText(
                  requireContext(),
                  R.string.contextProfileConditionBluetoothPermissionDenied,
                  Toast.LENGTH_LONG,
              )
              .show()
        }
      }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    setPreferencesFromResource(R.xml.preferences_context_profile, rootKey)

    findPreference<EditTextPreference>("name")?.setOnPreferenceChangeListener { _, newValue ->
      (newValue as String)
          .trim()
          .takeIf { it.isNotEmpty() }
          ?.let { name -> update { it.copy(name = name) } }
      false
    }
    findPreference<SwitchPreferenceCompat>("enabled")?.setOnPreferenceChangeListener { _, newValue
      ->
      update { it.copy(enabled = newValue as Boolean) }
      false
    }
    findPreference<ListPreference>("match")?.setOnPreferenceChangeListener { _, newValue ->
      update { it.copy(match = ConditionMatch.valueOf(newValue as String)) }
      false
    }
    findPreference<Preference>("addCondition")?.setOnPreferenceClickListener {
      chooseConditionType()
      true
    }
    findPreference<Preference>("staticLocation")?.setOnPreferenceClickListener {
      chooseStaticLocation()
      true
    }
    findPreference<ListPreference>("monitoring")?.setOnPreferenceChangeListener { _, newValue ->
      val monitoring = (newValue as String).toIntOrNull()?.let(MonitoringMode::getByValue)
      updateOverrides { it.copy(monitoring = monitoring) }
      false
    }
    findPreference<ListPreference>("locatorPriority")?.setOnPreferenceChangeListener { _, newValue
      ->
      val priority = LocatorPriority.entries.firstOrNull { it.name == newValue }
      updateOverrides { it.copy(locatorPriority = priority) }
      false
    }
    mapOf<String, (LocatorOverrides, Int?) -> LocatorOverrides>(
            "locatorInterval" to { overrides, value -> overrides.copy(locatorInterval = value) },
            "moveModeLocatorInterval" to
                { overrides, value ->
                  overrides.copy(moveModeLocatorInterval = value)
                },
            "locatorDisplacement" to
                { overrides, value ->
                  overrides.copy(locatorDisplacement = value)
                },
            "ping" to { overrides, value -> overrides.copy(ping = value) },
        )
        .forEach { (key, setOverride) ->
          findPreference<EditTextPreference>(key)?.apply {
            setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_NUMBER }
            summaryProvider =
                Preference.SummaryProvider<EditTextPreference> {
                  it.text.takeUnless { text -> text.isNullOrBlank() }
                      ?: getString(R.string.preferencesContextProfileNotChanged)
                }
            setOnPreferenceChangeListener { _, newValue ->
              updateOverrides { setOverride(it, (newValue as String).trim().toIntOrNull()) }
              false
            }
          }
        }
    findPreference<Preference>("delete")?.setOnPreferenceClickListener {
      confirmDelete()
      true
    }

    showProfile()
    lifecycleScope.launch {
      waypointNames = waypointsRepo.getAll().associate { it.tst.epochSecond to it.description }
      showProfile()
    }
  }

  private fun update(transform: (ContextProfile) -> ContextProfile) {
    profile?.run {
      preferences.contextProfileList = preferences.contextProfileList.replacing(transform(this))
    }
    showProfile()
  }

  private fun updateOverrides(transform: (LocatorOverrides) -> LocatorOverrides) = update {
    it.copy(overrides = transform(it.overrides))
  }

  private fun addCondition(condition: Condition) = update {
    if (condition in it.conditions) it else it.copy(conditions = it.conditions + condition)
  }

  private fun showProfile() {
    val profile =
        profile
            ?: run {
              parentFragmentManager.popBackStack()
              return
            }
    findPreference<EditTextPreference>("name")?.text = profile.name
    preferenceScreen.title = profile.name
    (activity as? AppCompatActivity)?.supportActionBar?.title = profile.name
    findPreference<SwitchPreferenceCompat>("enabled")?.isChecked = profile.enabled

    findPreference<ListPreference>("match")?.value = profile.match.name
    findPreference<PreferenceCategory>("conditions")?.run {
      title =
          getString(
              when (profile.match) {
                ConditionMatch.Any -> R.string.preferencesContextProfileCategoryConditionsAny
                ConditionMatch.All -> R.string.preferencesContextProfileCategoryConditionsAll
                ConditionMatch.None -> R.string.preferencesContextProfileCategoryConditionsNone
              }
          )
      removeAll()
      profile.conditions.forEach { condition ->
        addPreference(
            Preference(requireContext()).apply {
              title = requireContext().describeCondition(condition, waypointNames)
              isIconSpaceReserved = false
              setOnPreferenceClickListener {
                confirmRemoveCondition(condition)
                true
              }
            }
        )
      }
    }

    // Only show the settings that make a difference in the profile's monitoring mode
    val relevant = relevantOverrides(profile.overrides)
    LocatorOverride.entries.forEach {
      findPreference<Preference>(it.key)?.isVisible = it in relevant
    }
    profile.overrides.run {
      findPreference<Preference>("staticLocation")?.summary =
          staticLocation?.let {
            getString(
                R.string.preferencesContextProfileStaticLocationSet,
                it.toLatLng().toDisplayString(),
            )
          } ?: getString(R.string.preferencesContextProfileStaticLocationNotSet)
      findPreference<ListPreference>("monitoring")?.value = monitoring?.value?.toString() ?: ""
      findPreference<ListPreference>("locatorPriority")?.value = locatorPriority?.name ?: ""
      mapOf(
              "locatorInterval" to locatorInterval,
              "moveModeLocatorInterval" to moveModeLocatorInterval,
              "locatorDisplacement" to locatorDisplacement,
              "ping" to ping,
          )
          .forEach { (key, value) ->
            findPreference<EditTextPreference>(key)?.text = value?.toString() ?: ""
          }
    }
  }

  private fun confirmDelete() {
    MaterialAlertDialogBuilder(requireContext())
        .setMessage(R.string.preferencesContextProfileDeleteConfirmation)
        .setPositiveButton(R.string.delete) { _, _ ->
          preferences.contextProfileList = preferences.contextProfileList.without(profileId)
          parentFragmentManager.popBackStack()
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
  }

  private fun confirmRemoveCondition(condition: Condition) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(requireContext().describeCondition(condition, waypointNames))
        .setMessage(R.string.contextProfileConditionRemove)
        .setPositiveButton(R.string.delete) { _, _ ->
          update { it.copy(conditions = it.conditions - condition) }
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
  }

  private fun chooseConditionType() {
    val conditionTypes =
        listOf<Pair<Int, () -> Unit>>(
            R.string.contextProfileConditionTypeWifi to ::chooseWifiNetwork,
            R.string.contextProfileConditionTypeAnyWifi to { addCondition(Condition.AnyWifi) },
            R.string.contextProfileConditionTypeBluetooth to
                {
                  if (connectedBluetoothDevices.hasPermission()) {
                    chooseBluetoothDevice()
                  } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bluetoothPermissionRequest.launch(Manifest.permission.BLUETOOTH_CONNECT)
                  }
                },
            R.string.contextProfileConditionTypeCharging to { addCondition(Condition.Charging) },
            R.string.contextProfileConditionTypeChargingFrom to ::chooseChargingSource,
            R.string.contextProfileConditionTypeRegion to ::chooseRegion,
        )
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.preferencesContextProfileAddCondition)
        .setItems(conditionTypes.map { getString(it.first) }.toTypedArray()) { _, which ->
          conditionTypes[which].second()
        }
        .show()
  }

  /** Pick any number of nearby networks, or type one in */
  private fun chooseWifiNetwork() {
    val alreadyAdded = profile?.conditions?.filterIsInstance<Condition.WifiSsid>()?.map { it.ssid }
    val networks =
        (listOfNotNull(wifiInfoProvider.getSSID()?.takeIfKnownSSID()) +
                wifiInfoProvider.nearbySSIDs())
            .distinct()
            .filterNot { alreadyAdded?.contains(it) == true }
    if (networks.isEmpty()) {
      enterWifiNetwork()
      return
    }
    val checked = BooleanArray(networks.size)
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.contextProfileConditionTypeWifi)
        .setMultiChoiceItems(networks.toTypedArray(), checked) { _, which, isChecked ->
          checked[which] = isChecked
        }
        .setPositiveButton(android.R.string.ok) { _, _ ->
          networks
              .filterIndexed { index, _ -> checked[index] }
              .forEach { addCondition(Condition.WifiSsid(it)) }
        }
        .setNeutralButton(R.string.contextProfileConditionWifiOther) { _, _ -> enterWifiNetwork() }
        .setNegativeButton(R.string.cancel, null)
        .show()
  }

  private fun enterWifiNetwork() {
    val layout = layoutInflater.inflate(R.layout.ui_context_profile_wifi_dialog, null)
    MaterialAlertDialogBuilder(requireContext())
        .setView(layout)
        .setPositiveButton(android.R.string.ok) { _, _ ->
          layout.findViewById<EditText>(R.id.ssid).text.toString().trim().let {
            if (it.isNotEmpty()) addCondition(Condition.WifiSsid(it))
          }
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
  }

  @SuppressLint("MissingPermission") // Checked by bondedDevices
  private fun chooseBluetoothDevice() {
    val devices = connectedBluetoothDevices.bondedDevices()
    if (devices.isEmpty()) {
      showMessage(R.string.contextProfileConditionNoBluetoothDevices)
      return
    }
    val names = devices.map { it.name ?: it.address }
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.contextProfileConditionTypeBluetooth)
        .setItems(names.toTypedArray()) { _, which ->
          addCondition(Condition.BluetoothDevice(devices[which].address, names[which]))
        }
        .show()
  }

  private fun chooseChargingSource() {
    val sources = ChargingSource.entries
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.contextProfileConditionTypeChargingFrom)
        .setItems(sources.map { getString(it.labelResource) }.toTypedArray()) { _, which ->
          addCondition(Condition.ChargingFrom(sources[which]))
        }
        .show()
  }

  private fun chooseRegion() {
    lifecycleScope.launch {
      val waypoints = waypointsRepo.getAll()
      if (waypoints.isEmpty()) {
        showMessage(R.string.contextProfileConditionNoRegions)
        return@launch
      }
      MaterialAlertDialogBuilder(requireContext())
          .setTitle(R.string.contextProfileConditionTypeRegion)
          .setItems(waypoints.map { it.description }.toTypedArray()) { _, which ->
            addCondition(Condition.InRegion(waypoints[which].tst.epochSecond))
          }
          .show()
    }
  }

  private fun chooseStaticLocation() {
    val current = profile?.overrides?.staticLocation
    pickLocation.launch(
        LocationPickerActivity.PickLocation.Input(
            current?.toLatLng(),
            canClear = current != null,
        )
    )
  }

  /** Keeps any accuracy that was configured some other way */
  private fun setStaticLocation(latitude: Double, longitude: Double) = updateOverrides {
    val location = StaticLocation(latitude.roundedForStorage(), longitude.roundedForStorage())
    it.copy(
        staticLocation =
            it.staticLocation?.let { current -> location.copy(accuracy = current.accuracy) }
                ?: location
    )
  }

  private fun showMessage(message: Int) {
    MaterialAlertDialogBuilder(requireContext())
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .show()
  }

  // Six decimal places is about 10cm, which is plenty
  private fun Double.roundedForStorage(): Double = (this * 1_000_000).roundToLong() / 1_000_000.0

  companion object {
    const val PROFILE_ID = "profileId"
  }
}
