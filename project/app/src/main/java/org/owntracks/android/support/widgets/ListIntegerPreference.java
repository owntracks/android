package org.owntracks.android.support.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.afollestad.materialdialogs.prefs.MaterialListPreference;

public class ListIntegerPreference extends MaterialListPreference
{
    public ListIntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListIntegerPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean persistString(String value) {
        return value != null && persistInt(Integer.valueOf(value));
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if(getSharedPreferences().contains(getKey())) {
            int intValue = getPersistedInt(0);
            return String.valueOf(intValue);
        } else {
            return defaultReturnValue;
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return String.valueOf(a.getInt(index, 0));
    }


}
