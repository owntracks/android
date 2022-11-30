package org.owntracks.android.model.messages

import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.math.MathContext
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import org.jetbrains.annotations.NotNull
import org.owntracks.android.location.LatLng
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
    override val numberOfRetries: Int = 10_080 // This should last a week at 1 attempt per minute

    @JsonProperty("t")
    var trigger: String? = null

    @JsonProperty("batt")
    var battery = 0

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
        get() = "${latitude.roundToSignificantDigits(6)}, ${longitude.roundToSignificantDigits(6)}"

    @get:JsonIgnore
    var hasGeocode: Boolean = false
        private set

    fun toLatLng() = LatLng(latitude, longitude)

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
    override fun toString(): String {
        return String.format("Location id=%s: (%s,%s)", this.messageId, latitude, longitude)
    }

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
                    } else 0
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
        const val REPORT_TYPE_USER = "u"
        const val REPORT_TYPE_RESPONSE = "r"
        const val REPORT_TYPE_CIRCULAR = "c"
        const val REPORT_TYPE_PING = "p"

        @JvmField
        val REPORT_TYPE_DEFAULT: String? = null
        const val CONN_TYPE_OFFLINE = "o"
        const val CONN_TYPE_WIFI = "w"
        const val CONN_TYPE_MOBILE = "m"
    }

    private fun Double.roundToSignificantDigits(digits: Int): String =
        BigDecimal(this).round(MathContext(digits))
            .toString()
}
