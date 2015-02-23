package org.owntracks.android.support;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class EditIntegerPreference extends EditTextPreference{

    public EditIntegerPreference(Context c) {
        super(c);
    }

    public EditIntegerPreference(Context c, AttributeSet attrs) {
        super(c, attrs);
    }

    public EditIntegerPreference(Context c, AttributeSet attrs, int defStyle) {
        super(c, attrs, defStyle);
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


}
