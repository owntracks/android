package org.owntracks.android.model

import com.fasterxml.jackson.annotation.JsonValue

enum class BatteryStatus(val value: Int) {
    /**
     * The owntracks model for battery status
     */
    UNKNOWN(0), UNPLUGGED(1), CHARGING(2), FULL(3);
    @JsonValue
    fun getVal(): Int {
        return value
    }
}