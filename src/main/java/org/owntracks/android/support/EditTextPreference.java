package org.owntracks.android.support;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.afollestad.materialdialogs.util.DialogUtils;

import org.owntracks.android.R;

public class EditTextPreference extends DialogPreference {
    private String hint;

    public void setHint(String hint) {
        this.hint = hint;
    }


    private int mColor = 0;
    private EditText mEditText;
    private String mValue;

    public EditText getEditText() {
        return mEditText;
    }

    public void setValue(String value) {
        mValue = value;
    }

    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();
        if(persistString(text));
            setValue(text);
        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    public EditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            mColor = DialogUtils.resolveColor(context, R.attr.colorAccent);
        mEditText = new EditText(context, attrs);
    }

    public EditTextPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void showDialog(Bundle state) {
        Context context = getContext();

        // Set up our builder
        MaterialDialog.Builder mBuilder = new MaterialDialog.Builder(getContext())
                .title(getDialogTitle())
                .icon(getDialogIcon())
                .positiveText(getPositiveButtonText())
                .negativeText(getNegativeButtonText())
                .callback(callback)
                .content(getDialogMessage());

        // Create our layout, put the EditText inside, then add to dialog
        ViewGroup layout = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.md_input_dialog_stub, null);
        mEditText.setText(mValue);

        if(hint != null)
            getEditText().setHint(this.hint);

        if (mEditText.getParent() != null)
            ((ViewGroup) mEditText.getParent()).removeView(mEditText);
        layout.addView(mEditText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Color our EditText if need be. Lollipop does it by default
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            mEditText.getBackground().setColorFilter(mColor, PorterDuff.Mode.SRC_ATOP);

        TextView message = (TextView) layout.findViewById(android.R.id.message);
        if (getDialogMessage() != null && getDialogMessage().toString().length() > 0) {
            message.setVisibility(View.VISIBLE);
            message.setText(getDialogMessage());
        } else {
            message.setVisibility(View.GONE);
        }
        mBuilder.customView(layout, false);

        // Create the dialog
        MaterialDialog mDialog = mBuilder.build();
        if (state != null)
            mDialog.onRestoreInstanceState(state);

        // Show soft keyboard
        requestInputMethod(mDialog);

        mDialog.setOnDismissListener(this);
        mDialog.show();
    }

    /**
     * Callback listener for the MaterialDialog. Positive button checks with
     * OnPreferenceChangeListener before committing user entered text
     */
    private final MaterialDialog.ButtonCallback callback = new MaterialDialog.ButtonCallback() {
        @Override
        public void onPositive(MaterialDialog dialog) {
            Log.v(this.toString(), "onPositive");
            String value = mEditText.getText().toString();
            if (callChangeListener(value) && isPersistent()) {
                Log.v(this.toString(), "save");
                setText(value);
            }
        }
    };

    /**
     * Copied from DialogPreference.java
     */
    private void requestInputMethod(Dialog dialog) {
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    /**
     * Called when the default value attribute needs to be read
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    /**
     * Called on initialization, defaultValue populated only if onGetDefaultValue is overriden
     */
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setValue(restorePersistedValue ? getPersistedString("") : defaultValue.toString());
    }



    //@Override
    //protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    //    setValue(restorePersistedValue ? getPersistedString("") : "");
    //}

}
