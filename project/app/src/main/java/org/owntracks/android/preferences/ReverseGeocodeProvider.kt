package org.owntracks.android.preferences

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
        fun getByValue(value: String): ReverseGeocodeProvider =
            ReverseGeocodeProvider.values().firstOrNull { it.value == value } ?: DEVICE
    }
}
