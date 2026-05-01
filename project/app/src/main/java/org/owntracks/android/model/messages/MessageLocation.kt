package org.owntracks.android.model.messages

import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.doubleOrNull
import org.jetbrains.annotations.NotNull
import org.owntracks.android.model.BatteryStatus
import org.owntracks.android.net.WifiInfoProvider
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@SerialName(MessageLocation.TYPE)
open class MessageLocation(
    @Transient
    private val messageWithCreatedAtImpl: MessageWithCreatedAt = MessageCreatedAtNow(RealClock()),
    @Transient private val messageWithId: MessageWithId = MessageWithRandomId(),
) : MessageBase(), MessageWithCreatedAt, MessageWithId {

  override val numberOfRetries: Int =
      100_000 // This should last a few weeks at 1 attempt per minute

  @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
  @SerialName("created_at")
  override var createdAt:
      @kotlinx.serialization.Serializable(with = InstantEpochSecondsSerializer::class)
      Instant =
      messageWithCreatedAtImpl.createdAt

  @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
  @SerialName("_id")
  override var messageId: MessageId = messageWithId.messageId

  @SerialName("source") var source: String? = null

  @SerialName("t") var trigger: ReportType = ReportType.DEFAULT

  @SerialName("batt") var battery: Int? = null

  @SerialName("bs") var batteryStatus: BatteryStatus? = null

  @SerialName("acc") var accuracy = 0

  @SerialName("vac") var verticalAccuracy = 0

  @SerialName("lat") var latitude = 0.0

  @SerialName("lon") var longitude = 0.0

  @Serializable(with = LenientIntSerializer::class) @SerialName("alt") var altitude = 0

  @SerialName("vel") var velocity = 0

  @SerialName("cog") var bearing = 0

  @SerialName("tst") var timestamp: Long = 0

  @SerialName("m") var monitoringMode: MonitoringMode? = null

  var conn: String? = null

  @SerialName("inregions") var inregions: List<String>? = null

  @SerialName("BSSID") var bssid: String? = null

  @SerialName("SSID") var ssid: String? = null

  @SerialName("tid") var trackerId: String? = null

  @SerialName("address") var address: String? = null

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

    /** Accepts both integer and float JSON values for Int fields (some clients send `0.0`). */
    object LenientIntSerializer : JsonTransformingSerializer<Int>(Int.serializer()) {
      override fun transformDeserialize(element: JsonElement): JsonElement =
          if (element is JsonPrimitive &&
              element.content != "null" &&
              element.doubleOrNull != null) {
            JsonPrimitive(element.doubleOrNull!!.toInt())
          } else {
            element
          }
    }

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

  @Serializable(with = ReportType.ReportTypeSerializer::class)
  enum class ReportType(val value: String) {
    USER("u"), // Explicitly sent by the user
    RESPONSE("r"), // Triggered by a remote reportLocation command
    CIRCULAR("c"), // Region enter / leave event
    PING("p"), // Issued by the periodic ping worker
    TIMER("t"), // Generated by iOS devices
    BEACON("b"), // Generated by iOS beacons
    IOS_FREQUENT_LOCATIONS("v"), // Generated by iOS frequent locations
    IOS_FOLLOW_CIRCULAR("C"), // Generated by iOS follow circular region
    SIGNIFICANT_MOTION("m"), // Triggered by significant motion sensor
    BEACON_REGION("e"), // Beacon region
    DEFAULT("");

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

    object ReportTypeSerializer : KSerializer<ReportType> {
      override val descriptor: SerialDescriptor =
          PrimitiveSerialDescriptor("ReportType", PrimitiveKind.STRING)

      override fun deserialize(decoder: Decoder): ReportType {
        val v = decoder.decodeString()
        return entries.firstOrNull { it.value == v } ?: DEFAULT
      }

      override fun serialize(encoder: Encoder, value: ReportType) {
        encoder.encodeString(value.value)
      }
    }
  }
}

fun MessageLocation.addWifi(wifiInfoProvider: @NotNull WifiInfoProvider) {
  if (wifiInfoProvider.isConnected()) {
    bssid = wifiInfoProvider.getBSSID()
    ssid = wifiInfoProvider.getSSID()
  }
}
