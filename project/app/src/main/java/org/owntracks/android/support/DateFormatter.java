package org.owntracks.android.support;

import android.text.format.DateUtils;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateFormatter {
    private static final SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault());
    private static final SimpleDateFormat dateFormaterToday = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public static String formatDate(long tstSeconds) {
        return formatDate(new Date(TimeUnit.SECONDS.toMillis(tstSeconds)));
    }

    public static String formatDate(@NonNull Date d) {
        if(DateUtils.isToday(d.getTime())) {
            return dateFormaterToday.format(d);
        } else {
            return dateFormater.format(d);
        }
    }
}
