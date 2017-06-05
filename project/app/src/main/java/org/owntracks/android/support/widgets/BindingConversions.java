package org.owntracks.android.support.widgets;

import android.databinding.BindingAdapter;
import android.databinding.BindingConversion;
import android.widget.TextView;

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

}
