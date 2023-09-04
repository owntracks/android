package org.owntracks.android.model.messages

import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import com.fasterxml.jackson.annotation.*
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import org.jetbrains.annotations.NotNull
import org.owntracks.android.location.roundForDisplay
import org.owntracks.android.model.BatteryStatus
import org.owntracks.android.model.FusedContact
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.services.WifiInfoProvider

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "_type"
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
open class MessageLocation(private val dep: MessageWithCreatedAt = MessageCreatedAtNow(RealClock())) :
    MessageBase(), MessageWithCreatedAt by dep {

    @JsonIgnore
    override val numberOfRetries: Int = 100_000 // This should last a few weeks at 1 attempt per minute

    @JsonProperty("t")
    var trigger: ReportType = ReportType.DEFAULT

    @JsonProperty("batt")
    var battery: Int? = null

    @JsonProperty("bs")
    var batteryStatus: BatteryStatus? = null

    @JsonProperty("acc")
    var accuracy = 0

    @JsonProperty("vac")
    var verticalAccuracy = 0

    @JsonProperty("lat")
    var latitude = 0.0

    @JsonProperty("lon")
    var longitude = 0.0

    @JsonProperty("alt")
    var altitude = 0

    @JsonProperty("vel")
    var velocity = 0

    @JsonProperty("tst")
    var timestamp: Long = 0

    @JsonProperty("m")
    var monitoringMode: MonitoringMode? = null

    private var _contact: WeakReference<FusedContact?>? = null
    var conn: String? = null

    @JsonProperty("inregions")
    var inregions: List<String>? = null

    @JsonProperty("BSSID")
    var bssid: String? = null

    @JsonProperty("SSID")
    var ssid: String? = null

    @set:JsonIgnore
    @get:JsonIgnore
    var geocode: String? = null
        get() {
            return field ?: fallbackGeocode
        }
        set(value) {
            field = value
            hasGeocode = true
            notifyContactPropertyChanged()
        }

    @get:JsonIgnore
    internal val fallbackGeocode: String
        get() = "${latitude.roundForDisplay()}, ${longitude.roundForDisplay()}"

    @get:JsonIgnore
    var hasGeocode: Boolean = false
        private set

    fun setContact(contact: FusedContact?) {
        _contact = WeakReference(contact)
    }

    private fun notifyContactPropertyChanged() {
        _contact?.get()
            ?.notifyMessageLocationPropertyChanged()
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("tid")
    override var trackerId: String? = null
        set(value) {
            field = value
            notifyContactPropertyChanged()
        }

    override fun isValidMessage(): Boolean {
        return timestamp > 0
    }

    @JsonIgnore
    override fun toString(): String = "Location id=$messageId: ($latitude,$longitude) trigger=$trigger"

    override fun addMqttPreferences(preferences: Preferences) {
        topic = preferences.pubTopicLocations
        qos = preferences.pubQosLocations.value
        retained = preferences.pubRetainLocations
    }

    companion object {
        @SuppressLint("NewApi")
        @JvmStatic
        fun fromLocation(location: Location, sdk: Int = Build.VERSION.SDK_INT): MessageLocation =
            MessageLocation().apply {
                latitude = location.latitude
                longitude = location.longitude
                altitude = location.altitude.roundToInt()
                accuracy = location.accuracy.roundToInt()
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

        @JvmStatic
        fun fromLocationAndWifiInfo(
            location: @NotNull Location,
            wifiInfoProvider: WifiInfoProvider
        ): @NotNull MessageLocation =
            if (wifiInfoProvider.isConnected()) {
                fromLocation(location).apply {
                    ssid = wifiInfoProvider.getSSID()
                    bssid = wifiInfoProvider.getBSSID()
                }
            } else {
                fromLocation(location)
            }

        const val TYPE = "location"
        const val CONN_TYPE_OFFLINE = "o"
        const val CONN_TYPE_WIFI = "w"
        const val CONN_TYPE_MOBILE = "m"
    }

    enum class ReportType(@JsonValue val serialized: String) {
        USER("u"), // Explicitly sent by the user
        RESPONSE("r"), // Triggered by a remote reportLocation command
        CIRCULAR("c"), // Region enter / leave event
        PING("p"), // Issued by the periodic ping worker
        DEFAULT("")
    }
}
