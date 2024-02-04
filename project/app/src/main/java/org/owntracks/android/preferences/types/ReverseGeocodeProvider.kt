package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

enum class ReverseGeocodeProvider(@JsonValue val value: String) {
    NONE("None"),
    DEVICE("Device"),
    OPENCAGE("OpenCage");

    companion object {
        @JvmStatic
        @FromConfiguration
        fun getByValue(value: String): ReverseGeocodeProvider = entries.firstOrNull { it.value == value } ?: NONE
    }
}
