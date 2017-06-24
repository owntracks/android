package org.owntracks.android.support.widgets;

import android.databinding.BindingAdapter;
import android.databinding.BindingConversion;
import android.view.View;
import android.widget.TextView;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.MessageProcessor;

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

    @BindingAdapter("android:visibility")
    public static void setVisibility(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @BindingConversion
    public static int convertBooleanToVisibility(boolean visible) {
        return visible ? View.VISIBLE : View.GONE;
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

    private int convertModeIdToRadioButtonId(int modeId) {
        switch (modeId) {
            case App.MODE_ID_HTTP_PRIVATE:
                return R.id.radioModeHttpPrivate;
            case App.MODE_ID_MQTT_PRIVATE:
                return R.id.radioModeMqttPrivate;
            case App.MODE_ID_MQTT_PUBLIC:
            default:
                return R.id.radioModeMqttPublic;
        }
    }

    public int convertRadioButtonToModeId(int buttonId) {
        switch (buttonId) {
            case R.id.radioModeHttpPrivate:
                return App.MODE_ID_HTTP_PRIVATE;
            case R.id.radioModeMqttPrivate:
                return App.MODE_ID_MQTT_PRIVATE;
            case R.id.radioModeMqttPublic:
            default:
                return App.MODE_ID_MQTT_PUBLIC;
        }
    }
}
