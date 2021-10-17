package org.owntracks.android.support.widgets

import android.text.format.DateUtils
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import org.owntracks.android.R
import org.owntracks.android.data.EndpointState
import org.owntracks.android.location.Geofence
import java.text.DateFormat
import java.util.concurrent.TimeUnit


@BindingAdapter("android:text")
fun setText(view: TextView, state: LiveData<EndpointState?>?) {
    state?.value?.getLabel(view.context)

    view.text =
            if (state != null && state.value != null) state.value!!.getLabel(view.context) else view.context.getString(
                    R.string.na
            )
}

@BindingAdapter("relativeTimeSpanString")
fun setRelativeTimeSpanString(view: TextView, tstSeconds: Long) {
    when {
        tstSeconds == 0L -> {
            view.setText(R.string.valEmpty)
        }
        DateUtils.isToday(TimeUnit.SECONDS.toMillis(tstSeconds)) -> {
            view.text = DateFormat.getTimeInstance(DateFormat.SHORT)
                    .format(TimeUnit.SECONDS.toMillis(tstSeconds))
        }
        else -> {
            view.text = DateFormat.getDateInstance(DateFormat.SHORT)
                    .format(TimeUnit.SECONDS.toMillis(tstSeconds))
        }
    }
}

@BindingAdapter("lastTransition")
fun setLastTransition(view: TextView, transition: Int) {
    when (transition) {
        0 -> view.text = view.resources.getString(R.string.region_unknown)
        Geofence.GEOFENCE_TRANSITION_ENTER -> view.text =
                view.resources.getString(R.string.region_inside)
        Geofence.GEOFENCE_TRANSITION_EXIT -> view.text =
                view.resources.getString(R.string.region_outside)
    }
}
