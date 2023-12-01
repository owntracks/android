package org.owntracks.android.support

import android.text.format.DateUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateFormatter {
    @JvmStatic
    fun formatDate(tstMilliSeconds: Long): String {
        return formatDate(Instant.ofEpochMilli(tstMilliSeconds))
    }

    @JvmStatic
    fun formatDate(instant: Instant): String = if (DateUtils.isToday(instant.toEpochMilli())) {
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(instant)
    } else {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(instant)
    }
}
