package org.owntracks.android.model.messages

import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.NotNull
import org.owntracks.android.model.BatteryStatus
import org.owntracks.android.net.WifiInfoProvider
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@SerialName(MessageLocation.TYPE)
open class MessageLocation(
    private val messageWithCreatedAtImpl: MessageWithCreatedAt = MessageCreatedAtNow(RealClock()),
    private val messageWithId: MessageWithId = MessageWithRandomId(),
) :
    MessageBase(),
    MessageWithCreatedAt by messageWithCreatedAtImpl,
    MessageWithId by messageWithId {

  override val numberOfRetries: Int =
      100_000 // This should last a few weeks at 1 attempt per minute

  @SerialName("source") var source: String? = null

  @SerialName("t") var trigger: ReportType = ReportType.DEFAULT

  @SerialName("batt") var battery: Int? = null

  @SerialName("bs") var batteryStatus: BatteryStatus? = null

  @SerialName("acc") var accuracy = 0

  @SerialName("vac") var verticalAccuracy = 0

  @SerialName("lat") var latitude = 0.0

  @SerialName("lon") var longitude = 0.0

  @SerialName("alt") var altitude = 0

  @SerialName("vel") var velocity = 0

  @SerialName("cog") var bearing = 0

  @SerialName("tst") var timestamp: Long = 0

  @SerialName("m") var monitoringMode: MonitoringMode? = null

  var conn: String? = null

  @SerialName("inregions") var inregions: List<String>? = null

  @SerialName("BSSID") var bssid: String? = null

  @SerialName("SSID") var ssid: String? = null

  @SerialName("tid") var trackerId: String? = null

  override fun isValidMessage(): Boolean {
    return timestamp > 0
  }

  override fun toString(): String =
      "[MessageLocation id=$messageId ts=${Instant.fromEpochSeconds(timestamp)},lat=$latitude,long=$longitude,created_at=${createdAt},trigger=$trigger]"

  override fun annotateFromPreferences(preferences: Preferences) {
    topic = preferences.pubTopicLocations
    qos = preferences.pubQosLocations.value
    retained = preferences.pubRetainLocations
  }

  companion object {
    const val TYPE = "location"

    @SuppressLint("NewApi")
    @JvmStatic
    fun fromLocation(location: Location, sdk: Int = Build.VERSION.SDK_INT): MessageLocation =
        MessageLocation().apply {
          latitude = location.latitude
          longitude = location.longitude
          altitude = location.altitude.roundToInt()
          accuracy = location.accuracy.roundToInt()
          bearing = location.bearing.roundToInt()
          timestamp = TimeUnit.MILLISECONDS.toSeconds(location.time)
          // Convert m/s to km/h
          velocity = if (location.hasSpeed()) ((location.speed * 3.6).toInt()) else 0
          verticalAccuracy =
              if (sdk >= Build.VERSION_CODES.O && location.hasVerticalAccuracy()) {
                location.verticalAccuracyMeters.toInt()
              } else {
                0
              }
        }
  }

  enum class ConnectionType(val value: String) {
    WIFI("w"),
    MOBILE("m"),
    OFFLINE("o")
  }

  enum class ReportType(val value: String) {
    DEFAULT("u"), // manual
    PING("p"), // ping
    BEACON("b"), // beacon
    RESPONSE("r"), // response
    USER("u"), // user
    TIMER("t"), // timer
    CIRCULAR("c"), // circular region
    BEACON_REGION("e"); // beacon region

    companion object {
      fun fromChar(s: String?): ReportType {
        for (b in entries) {
          if (b.value.equals(s, ignoreCase = true)) {
            return b
          }
        }
        return DEFAULT
      }
    }
  }
}

fun MessageLocation.addWifi(wifiInfoProvider: @NotNull WifiInfoProvider) {
  if (wifiInfoProvider.isWifiConnected) {
    bssid = wifiInfoProvider.bssid
    ssid = wifiInfoProvider.ssid
  }
}
