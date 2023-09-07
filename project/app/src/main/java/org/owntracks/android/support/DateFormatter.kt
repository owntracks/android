package org.owntracks.android.support

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    @JvmStatic
    fun formatDate(tstMilliSeconds: Long): String {
        return formatDate(Date(tstMilliSeconds))
    }

    @JvmStatic
    fun formatDate(d: Date): String {
        return if (DateUtils.isToday(d.time)) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(d)
        } else {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(d)
        }
    }
}
