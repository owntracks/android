package org.owntracks.android.preferences

import com.fasterxml.jackson.annotation.JsonValue

enum class ConnectionMode(val value: Int) {
    MQTT(0),
    HTTP(3);

    @JsonValue
    fun getVal(): Int {
        return value
    }
}
