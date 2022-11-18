package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

enum class AppTheme(@JsonValue val value: Int) {
    LIGHT(0),
    DARK(1),
    AUTO(2);

    companion object {
        @JvmStatic
        @FromConfiguration
        fun getByValue(value: Int): AppTheme =
            AppTheme.values()
                .firstOrNull { it.value == value } ?: LIGHT
    }
}
