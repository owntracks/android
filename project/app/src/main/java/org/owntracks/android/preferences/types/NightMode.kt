package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

enum class NightMode(@JsonValue val value: Int) {
    DISABLE(0),
    ENABLE(1),
    AUTO(2);

    companion object {
        @JvmStatic
        fun getByValue(value: Int): NightMode =
            NightMode.values()
                .firstOrNull { it.value == value } ?: DISABLE
    }
}
