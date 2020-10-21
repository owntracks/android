package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.*
import org.owntracks.android.model.BatteryStatus
import org.owntracks.android.model.FusedContact
import org.owntracks.android.support.Preferences
import java.lang.ref.WeakReference

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class MessageLocation : MessageBase() {
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
    private var geocoder: String? = null
    private var _contact: WeakReference<FusedContact?>? = null
    var conn: String? = null

    @JsonProperty("inregions")
    var inregions: List<String>? = null
    @JsonIgnore
    fun getGeocoder(): String? {
        return if (hasGeocoder()) geocoder else geocoderFallback
    }

    @JsonIgnore
    fun setGeocoder(geocoder: String?) {
        this.geocoder = geocoder
        notifyContactPropertyChanged()
    }

    @get:JsonIgnore
    val geocoderFallback: String
        get() = "$latitude : $longitude"

    @JsonIgnore
    fun hasGeocoder(): Boolean {
        return geocoder != null
    }

    fun setContact(contact: FusedContact?) {
        _contact = WeakReference(contact)
    }

    private fun notifyContactPropertyChanged() {
        if (_contact != null && _contact!!.get() != null) _contact!!.get()!!.notifyMessageLocationPropertyChanged()
    }

    public override fun setTrackerId(trackerId: String) {
        super.setTrackerId(trackerId)
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
        qos = preferences.pubQosLocations
        retained = preferences.pubRetainLocations
    }

    companion object {
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
}