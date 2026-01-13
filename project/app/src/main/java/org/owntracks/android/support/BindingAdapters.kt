package org.owntracks.android.support

import android.text.format.DateUtils
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputEditText
import java.text.DateFormat
import java.time.Instant
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude
import org.owntracks.android.location.roundForDisplay

@BindingAdapter("relativeTimeSpanString")
fun TextView.setRelativeTimeSpanString(instant: Instant?) {
    text =
        if (instant == null || instant == Instant.MIN) {
            ""
        } else if (DateUtils.isToday(instant.toEpochMilli())) {
            DateFormat.getTimeInstance(DateFormat.SHORT).format(instant.toEpochMilli())
        } else {
            DateFormat.getDateInstance(DateFormat.SHORT).format(instant.toEpochMilli())
        }
}

@BindingAdapter("relativeTimeSpanString")
fun TextView.setRelativeTimeSpanStringFromLong(epochSeconds: Long?) {
    val instant = epochSeconds?.run(Instant::ofEpochSecond) ?: Instant.MIN
    text =
        if (instant == Instant.MIN) {
            ""
        } else if (DateUtils.isToday(instant.toEpochMilli())) {
            DateFormat.getTimeInstance(DateFormat.SHORT).format(instant.toEpochMilli())
        } else {
            DateFormat.getDateInstance(DateFormat.SHORT).format(instant.toEpochMilli())
        }
}

@BindingAdapter("android:text")
fun TextInputEditText.setLatitude(latitude: Latitude) {
    setText(latitude.value.roundForDisplay())
}

@BindingAdapter("android:text")
fun TextInputEditText.setLongitude(longitude: Longitude) {
    setText(longitude.value.roundForDisplay())
}
