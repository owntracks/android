package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

enum class MqttProtocolLevel(private val value: Int) {
    MQTT_3_1(3),
    MQTT_3_1_1(4);

    @JsonValue
    fun getVal(): Int {
        return value
    }
}
