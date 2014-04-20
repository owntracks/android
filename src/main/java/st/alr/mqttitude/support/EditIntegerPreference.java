package st.alr.mqttitude.support;

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

public class EditIntegerPreference extends EditTextPreference{
    private String mString;
    private Integer mInteger;

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
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setText(restoreValue ? String.valueOf(getPersistedInt(mInteger)) : (String) defaultValue);
    }

    @Override
    public boolean shouldDisableDependents() {
        return TextUtils.isEmpty(mString) || super.shouldDisableDependents();
    }

    /**
     * Saves the text to the {@link android.content.SharedPreferences}.
     *
     * @param text The text to save
     */
    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();

        mString = text;
        try {
            mInteger = Integer.parseInt(mString);

        } catch (NumberFormatException e) {
            mInteger = 1337;
        }

        persistInt(mInteger);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    /**
     * Gets the text from the {@link android.content.SharedPreferences}.
     *
     * @return The current preference value.
     */

    @Override
    public String getText() {
        return String.valueOf(mInteger);
    }

}
