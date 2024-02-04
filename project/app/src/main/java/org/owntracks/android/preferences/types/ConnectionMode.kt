package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

enum class ConnectionMode(@JsonValue val value: Int) {
    MQTT(0),
    HTTP(3);

    companion object {
        @JvmStatic
        @FromConfiguration
        fun getByValue(value: Int): ConnectionMode =
            entries
                .firstOrNull { it.value == value } ?: MQTT
    }
}
