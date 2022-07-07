package org.owntracks.android.support

import com.fasterxml.jackson.annotation.JsonValue

enum class MonitoringMode(@JsonValue val mode: Int) {
    QUIET(-1),
    MANUAL(0),
    SIGNIFICANT(1),
    MOVE(2);

    fun next(): MonitoringMode = when (this) {
        QUIET -> MANUAL
        MANUAL -> SIGNIFICANT
        SIGNIFICANT -> MOVE
        MOVE -> QUIET
    }

    companion object {
        @JvmStatic
        fun getByValue(value: Int) =
            values().firstOrNull { it.mode == value } ?: SIGNIFICANT
    }
}
