package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

enum class MqttQos(private val value: Int) {
    ZERO(0),
    ONE(1),
    TWO(2);

    @JsonValue
    fun getVal(): Int {
        return value
    }
}
