package org.owntracks.android.support.widgets;

import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingConversion;
import androidx.databinding.InverseMethod;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.owntracks.android.R;
import org.owntracks.android.location.geofencing.Geofence;
import org.owntracks.android.preferences.types.ConnectionMode;
import org.owntracks.android.services.MessageProcessorEndpointMqtt;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class BindingConversions {
    private static final String EMPTY_STRING = "";


    // XX to String
    @BindingConversion
    @InverseMethod("convertToInteger")
    public static String convertToString(@Nullable Integer d) {
        return d != null ? java.text.NumberFormat.getIntegerInstance().format(d) : EMPTY_STRING;
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

    @BindingAdapter("android:visibility")
    public static void setVisibility(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter("relativeTimeSpanString")
    public static void setRelativeTimeSpanString(TextView view, long tstSeconds) {
        if (tstSeconds == 0) {
            view.setText("");
        } else if (DateUtils.isToday(TimeUnit.SECONDS.toMillis(tstSeconds))) {
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

    @BindingAdapter("android:text")
    public static void setDate(TextView view, Date date) {
        if (date == null) {
            view.setText(R.string.na);
        } else {
            if (DateUtils.isToday(date.getTime())) {
                view.setText(new SimpleDateFormat("HH:mm", view.getTextLocale()).format(date));
            } else {
                view.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm", view.getTextLocale()).format(date));
            }
        }
    }

    @BindingAdapter("android:text")
    public static void setDate(TextView view, long date) {
        setDate(view, new Date(TimeUnit.SECONDS.toMillis(date)));
    }

    public static int convertModeIdToLabelResId(ConnectionMode modeId) {
        switch (modeId) {
            case HTTP:
                return R.string.mode_http_private_label;
            case MQTT:
            default:
                return R.string.mode_mqtt_private_label;
        }
    }
}
