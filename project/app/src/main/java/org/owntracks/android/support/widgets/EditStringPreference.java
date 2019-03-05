package org.owntracks.android.support.widgets;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.StringRes;

import android.preference.EditTextPreference;
import android.util.AttributeSet;


public class EditStringPreference extends EditTextPreference {
    EditStringPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditStringPreference(Context context) {
        super(context);
    }

    @Override
    protected void showDialog(Bundle s) {
        super.showDialog(s);
        getEditText().setHint(this.hint);
    }

        private String hint;
    public void setHint(String hint) {
        this.hint = hint;
    }

    public void setText(String text) {
        if(!shouldPersist())
            return;
        super.setText(text);
    }

    public EditStringPreference withPreferencesSummary(@StringRes int res) {
        setSummary(res);
        return this;
    }
    public EditStringPreference withDialogMessage(@StringRes int res) {
        setDialogMessage(res);
        return this;
    }
}