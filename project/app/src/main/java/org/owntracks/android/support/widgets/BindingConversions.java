package org.owntracks.android.support.widgets;

import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingConversion;
import androidx.databinding.InverseMethod;

import com.google.android.gms.location.Geofence;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.owntracks.android.R;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.services.MessageProcessorEndpointHttp;
import org.owntracks.android.services.MessageProcessorEndpointMqtt;

import java.text.DateFormat;
import java.util.concurrent.TimeUnit;

public class BindingConversions {
    private static final String EMPTY_STRING = "";


    // XX to String
    @BindingConversion
    @InverseMethod("convertToInteger")
    public static String convertToString(@Nullable Integer d) {
        return d != null ? d.toString() : EMPTY_STRING;
    }

    @BindingConversion
    @InverseMethod("convertToIntegerZeroIsEmpty")
    public static String convertToStringZeroIsEmpty(@Nullable Integer d) {
        return d != null && d > 0 ? d.toString() : EMPTY_STRING;
    }


    @BindingConversion
    public static String convertToString(@Nullable Long d) {
        return d != null ? d.toString() : EMPTY_STRING;
    }


    @BindingConversion
    public static String convertToString(boolean d) {
        return String.valueOf(d);
    }

    @BindingConversion
    public static String convertToString(String s) {
        return s != null ? s : EMPTY_STRING;
    }


    // XX to Integer
    @BindingConversion
    public static Integer convertToInteger(String d) {
        try {
            return Integer.parseInt(d);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @BindingConversion
    public static Integer convertToIntegerZeroIsEmpty(String d) {
        return convertToInteger(d);
    }

    // Misc
    @BindingAdapter({"android:text"})
    public static void setText(TextView view, MessageProcessor.EndpointState state) {
        view.setText(state != null ? state.getLabel(view.getContext()) : view.getContext().getString(R.string.na));
    }

    @BindingAdapter("met_helperText")
    public static void setVisibility(MaterialEditText view, String text) {
        view.setHelperText(text);
    }

    @BindingAdapter("android:visibility")
    public static void setVisibility(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter("relativeTimeSpanString")
    public static void setRelativeTimeSpanString(TextView view, long tstSeconds) {
        if (DateUtils.isToday(TimeUnit.SECONDS.toMillis(tstSeconds))) {
            view.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(TimeUnit.SECONDS.toMillis(tstSeconds)));
        } else {
            view.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(TimeUnit.SECONDS.toMillis(tstSeconds)));
        }
    }

    @BindingAdapter("lastTransition")
    public static void setLastTransition(TextView view, int transition) {
        switch (transition) {
            case 0:
                view.setText(view.getResources().getString(R.string.region_unknown));
                break;
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                view.setText(view.getResources().getString(R.string.region_inside));
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                view.setText(view.getResources().getString(R.string.region_outside));
                break;

        }
    }


    public static int convertModeIdToLabelResId(int modeId) {
        switch (modeId) {
            case MessageProcessorEndpointHttp.MODE_ID:
                return R.string.mode_http_private_label;
            case MessageProcessorEndpointMqtt.MODE_ID:
            default:
                return R.string.mode_mqtt_private_label;
        }
    }
}
