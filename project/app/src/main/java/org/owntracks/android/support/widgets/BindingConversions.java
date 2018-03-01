package org.owntracks.android.support.widgets;

import android.databinding.BindingAdapter;
import android.databinding.BindingConversion;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import com.rengwuxian.materialedittext.MaterialEditText;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.MessageProcessor;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class BindingConversions {
    private static final String EMPTY_STRING = "";
    @BindingConversion
    public static String convertDoubleToString(Double d) {
        return  d != null? d.toString() : EMPTY_STRING;

    }
    @BindingConversion
    @Deprecated
    public static Double convertStringToDouble(String d) {
        return d != null ? Double.parseDouble(d) : null;
    }

    @BindingConversion
    public static Double convertToDouble(String d) {
        return d != null ? Double.parseDouble(d) : null;
    }

    @BindingConversion
    @Deprecated
    public static String convertStringToString(String s) {
        return  s != null ? s : EMPTY_STRING;
    }

    @BindingConversion
    @Deprecated
    public static String convertIntegerToString(Integer d) {
        return  d != null? d.toString() : EMPTY_STRING;
    }

    @BindingConversion
    public static String convertToString(boolean d) {
        return  String.valueOf(d);
    }

    @BindingConversion
    public static String convertToString(Integer d) {
        return  d != null? d.toString() : EMPTY_STRING;
    }

    @BindingConversion
    public static String convertToString(String s) {
        return  s != null ? s : EMPTY_STRING;
    }


    @BindingAdapter({"android:text"})
    public static void setText(TextView view, MessageProcessor.EndpointState state) {
        view.setText(state != null ? state.getLabel(view.getContext()) : view.getContext().getString(R.string.na));
    }

    @BindingAdapter("app:met_helperText")
    public static void setVisibility(MaterialEditText view, String text) {
        view.setHelperText(text);
    }

    @BindingAdapter("android:visibility")
    public static void setVisibility(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter("app:relativeTimeSpanString")
    public static void setRelativeTimeSpanString(TextView view, long tstSeconds) {


        if(DateUtils.isToday(TimeUnit.SECONDS.toMillis(tstSeconds))) {
            view.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(TimeUnit.SECONDS.toMillis(tstSeconds)));
        } else {
            view.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(TimeUnit.SECONDS.toMillis(tstSeconds)));
        }
        //if (deltaMs < DateUtils.MINUTE_IN_MILLIS) {
        //    view.setText(R.string.timeNow);
        //} else if(deltaMs < DateUtils.HOUR_IN_MILLIS) {
        //    view.setText(String.format("%sm", TimeUnit.MILLISECONDS.toMinutes(deltaMs)));
        //} else if (deltaMs < DateUtils.DAY_IN_MILLIS) {
        //    view.setText(String.format("%sh", TimeUnit.MILLISECONDS.toHours(deltaMs)));
        //} else {
        //    view.setText(String.format("%sd", TimeUnit.MILLISECONDS.toDays(deltaMs)));
        //}
        //view.setText(DateUtils.getRelativeTimeSpanString(tst*1000, System.currentTimeMillis(), 1 ,DateUtils.FORMAT_ABBREV_ALL));
    }

    public static int convertModeIdToLabelResId(int modeId) {
        switch (modeId) {
            case App.MODE_ID_HTTP_PRIVATE:
                return R.string.mode_http_private_label;
            case App.MODE_ID_MQTT_PRIVATE:
                return R.string.mode_mqtt_private_label;
            case App.MODE_ID_MQTT_PUBLIC:
            default:
                return R.string.mode_mqtt_public_label;
        }
    }
}
