package org.owntracks.android.support;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

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
        if(value == null) {
            return false;
        } else {
            return persistInt(Integer.valueOf(value));
        }
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
