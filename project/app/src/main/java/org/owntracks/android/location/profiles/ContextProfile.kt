package org.owntracks.android.location.profiles

import android.location.Location
import android.os.SystemClock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.LocatorPriority
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode
import timber.log.Timber

/**
 * A named set of [LocatorOverrides] that applies while its [conditions] match the current
 * [DeviceContext], as decided by [match]. Profiles are held in priority order, and the first
 * matching one wins.
 */
@Serializable
data class ContextProfile(
    val id: String,
    val name: String,
    val conditions: List<Condition>,
    val overrides: LocatorOverrides,
    val enabled: Boolean = true,
    val match: ConditionMatch = ConditionMatch.Any,
) {
  /** A profile with no conditions never matches, rather than always matching under All or None */
  fun matches(context: DeviceContext): Boolean =
      enabled &&
          conditions.isNotEmpty() &&
          when (match) {
            ConditionMatch.Any -> conditions.any { it.matches(context) }
            ConditionMatch.All -> conditions.all { it.matches(context) }
            ConditionMatch.None -> conditions.none { it.matches(context) }
          }
}

@Serializable
enum class ChargingSource {
  /** Mains (AC) or USB */
  @SerialName("cable") Cable,
  @SerialName("wireless") Wireless,
  @SerialName("dock") Dock,
}

/** How many of a profile's conditions need to match for the profile to match */
@Serializable
enum class ConditionMatch {
  @SerialName("any") Any,
  @SerialName("all") All,
  @SerialName("none") None,
}

@Serializable
sealed interface Condition {
  fun matches(context: DeviceContext): Boolean

  @Serializable
  @SerialName("wifi")
  data class WifiSsid(val ssid: String) : Condition {
    override fun matches(context: DeviceContext): Boolean = context.wifiSsid == ssid
  }

  @Serializable
  @SerialName("anyWifi")
  data object AnyWifi : Condition {
    override fun matches(context: DeviceContext): Boolean = context.wifiSsid != null
  }

  /** [name] is only for display. Devices are matched on their [address]. */
  @Serializable
  @SerialName("bluetooth")
  data class BluetoothDevice(val address: String, val name: String) : Condition {
    override fun matches(context: DeviceContext): Boolean =
        context.bluetoothDeviceAddresses.any { it.equals(address, ignoreCase = true) }
  }

  /** Charging in any way */
  @Serializable
  @SerialName("charging")
  data object Charging : Condition {
    override fun matches(context: DeviceContext): Boolean = context.chargingSource != null
  }

  @Serializable
  @SerialName("chargingFrom")
  data class ChargingFrom(val source: ChargingSource) : Condition {
    override fun matches(context: DeviceContext): Boolean = context.chargingSource == source
  }

  @Serializable
  @SerialName("region")
  /**
   * Inside a waypoint's region. Waypoints are identified by their timestamp (in epoch seconds), as
   * they are when exported, because their local ids change when they're re-imported.
   */
  data class InRegion(val tst: Long) : Condition {
    override fun matches(context: DeviceContext): Boolean =
        context.enteredWaypointTsts.contains(tst)
  }
}

/** Locator preferences a profile replaces while active. A null field inherits the preference. */
@Serializable
data class LocatorOverrides(
    val monitoring: MonitoringMode? = null,
    val locatorInterval: Int? = null,
    val moveModeLocatorInterval: Int? = null,
    val locatorDisplacement: Int? = null,
    val locatorPriority: LocatorPriority? = null,
    val ping: Int? = null,
    val staticLocation: StaticLocation? = null,
)

/** The names of the preferences these override */
internal fun LocatorOverrides.overriddenPreferences(): Set<String> =
    listOfNotNull(
            monitoring?.let { Preferences::monitoring.name },
            locatorInterval?.let { Preferences::locatorInterval.name },
            moveModeLocatorInterval?.let { Preferences::moveModeLocatorInterval.name },
            locatorDisplacement?.let { Preferences::locatorDisplacement.name },
            locatorPriority?.let { Preferences::locatorPriority.name },
            ping?.let { Preferences::ping.name },
        )
        .toSet()

/** A location reported in place of real location updates, which are stopped while it is set. */
@Serializable
data class StaticLocation(val latitude: Double, val longitude: Double, val accuracy: Int = 10) {
  fun toLatLng(): LatLng = LatLng(latitude, longitude)

  /** A [Location] at this static location, as though it had just been found */
  fun toLocation(): Location =
      Location(PROVIDER).also {
        it.latitude = latitude
        it.longitude = longitude
        it.accuracy = accuracy.toFloat()
        it.time = System.currentTimeMillis()
        it.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
      }

  companion object {
    const val PROVIDER = "static"
  }
}

fun List<ContextProfile>.firstMatching(context: DeviceContext): ContextProfile? = firstOrNull {
  it.matches(context)
}

private val contextProfilesJson = Json { ignoreUnknownKeys = true }

internal fun encodeContextProfiles(profiles: List<ContextProfile>): String =
    contextProfilesJson.encodeToString(profiles)

/** Decodes stored profiles, falling back to none so that a bad config can't break locating. */
internal fun decodeContextProfiles(json: String): List<ContextProfile> =
    if (json.isBlank()) {
      emptyList()
    } else {
      try {
        contextProfilesJson.decodeFromString<List<ContextProfile>>(json)
      } catch (e: IllegalArgumentException) {
        // Including the SerializationException that bad JSON gives
        Timber.w(e, "Unable to decode context profiles, ignoring them")
        emptyList()
      }
    }
