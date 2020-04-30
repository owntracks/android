package org.owntracks.android.support.widgets;

import android.content.Context;

import android.util.AttributeSet;

import timber.log.Timber;

public class EditIntegerPreference extends EditStringPreference {

    public EditIntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditIntegerPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean persistString(String value) {
        if(value == null || "".equals(value)) {
            getSharedPreferences().edit().remove(getKey()).apply();
            return true;
        }
        try {
            return persistInt(Integer.valueOf(value));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if(getSharedPreferences().contains(getKey())) {
            try {
                int intValue = getPersistedInt(0);
                return String.valueOf(intValue);
            }  catch (ClassCastException e) {
                Timber.e("Error retriving string preference %s, returning default", defaultReturnValue);
                return defaultReturnValue;
            }
        } else {
            return defaultReturnValue;
        }
    }
}
