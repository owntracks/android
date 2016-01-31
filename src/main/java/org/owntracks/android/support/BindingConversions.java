package org.owntracks.android.support;

import android.databinding.BindingConversion;

public class BindingConversions {
    @BindingConversion
    public static String convertDoubleToString(Double d) {
        return  d != null? d.toString() : null;

    }
    @BindingConversion
    public static Double convertStringToDouble(String d) {
        return d != null ? Double.parseDouble(d) : null;
    }
    @BindingConversion
    public static String convertStringToString(String s) {
        return  s != null ? s : "";
    }
    @BindingConversion
    public static String convertIntegerToString(Integer d) {
        return  d != null? d.toString() : null;
    }


}
