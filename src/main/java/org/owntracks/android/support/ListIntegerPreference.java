package org.owntracks.android.support;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class ListIntegerPreference extends ListPreference
{
    public ListIntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListIntegerPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean persistString(String value) {
        return value != null && persistString(value);
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
