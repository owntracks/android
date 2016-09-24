package org.owntracks.android.support.widgets;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;

import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;

public class EditStringPreference extends MaterialEditTextPreference {
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
}