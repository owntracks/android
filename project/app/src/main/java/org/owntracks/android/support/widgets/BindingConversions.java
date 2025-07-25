//package org.owntracks.android.support.widgets;
//
//import android.text.format.DateUtils;
//import android.view.View;
//import android.widget.TextView;
//
//import androidx.annotation.Nullable;
//import androidx.databinding.BindingAdapter;
//import androidx.databinding.BindingConversion;
//import androidx.databinding.InverseMethod;
//
//import org.owntracks.android.R;
//import org.owntracks.android.location.LatLng;
//import org.owntracks.android.location.geofencing.Geofence;
//import org.owntracks.android.preferences.types.ConnectionMode;
//
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.concurrent.TimeUnit;
//
//public class BindingConversions {
//    private static final String EMPTY_STRING = "";
//
//    @BindingConversion
//    public static String convertToString(@Nullable Double d) {
//        return d != null ? d.toString() : EMPTY_STRING;
//    }
//
//    @BindingConversion
//    public static String convertToString(String s) {
//        return s != null ? s : EMPTY_STRING;
//    }
//
//    @BindingConversion
//    public static String convertLatLngToString(LatLng latLng) {
//        if (latLng == null) {
//            return "";
//        } else {
//            return latLng.toDisplayString();
//        }
//    }
//
//    @BindingAdapter("android:visibility")
//    public static void setVisibility(View view, boolean visible) {
//        view.setVisibility(visible ? View.VISIBLE : View.GONE);
//    }
//
//    @BindingAdapter("lastTransition")
//    public static void setLastTransition(TextView view, int transition) {
//        switch (transition) {
//            case 0 -> view.setText(view.getResources().getString(R.string.waypoint_region_unknown));
//            case Geofence.GEOFENCE_TRANSITION_ENTER ->
//                    view.setText(view.getResources().getString(R.string.waypoint_region_inside));
//            case Geofence.GEOFENCE_TRANSITION_EXIT ->
//                    view.setText(view.getResources().getString(R.string.waypoint_region_outside));
//        }
//    }
//
//    @BindingAdapter("android:text")
//    public static void setDate(TextView view, Date date) {
//        if (date == null) {
//            view.setText(R.string.na);
//        } else {
//            if (DateUtils.isToday(date.getTime())) {
//                view.setText(new SimpleDateFormat("HH:mm", view.getTextLocale()).format(date));
//            } else {
//                view.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm", view.getTextLocale()).format(date));
//            }
//        }
//    }
//
//    @BindingAdapter("android:text")
//    public static void setDate(TextView view, long date) {
//        setDate(view, new Date(TimeUnit.SECONDS.toMillis(date)));
//    }
//
//}
