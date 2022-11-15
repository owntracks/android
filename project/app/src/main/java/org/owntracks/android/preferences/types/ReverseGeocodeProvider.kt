package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

enum class ReverseGeocodeProvider(val value: String) {
    NONE("None"),
    DEVICE("Device"),
    OPENCAGE("OpenCage");

    @JsonValue
    fun getVal(): String {
        return value
    }

    companion object {
        @JvmStatic
        @FromConfiguration
        fun getByValue(value: String): ReverseGeocodeProvider =
            ReverseGeocodeProvider.values().firstOrNull { it.value == value } ?: NONE
    }
}
