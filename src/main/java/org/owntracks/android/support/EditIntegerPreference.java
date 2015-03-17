package org.owntracks.android.support;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;

public class EditIntegerPreference extends org.owntracks.android.support.EditTextPreference {

    public EditIntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditIntegerPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean persistString(String value) {
        if(value == null || "".equals(value)) {
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.remove(getKey());
            editor.commit();
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
            int intValue = getPersistedInt(0);
            return String.valueOf(intValue);
        } else {
            return defaultReturnValue;
        }
    }

}
