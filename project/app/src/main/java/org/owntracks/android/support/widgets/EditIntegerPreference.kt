package org.owntracks.android.support.widgets

import android.content.Context
import android.util.AttributeSet
import com.takisoft.preferencex.EditTextPreference
import timber.log.Timber

class EditIntegerPreference(context: Context?, attrs: AttributeSet?) :
    EditTextPreference(context, attrs) {
    override fun persistString(value: String?): Boolean {
        if (value == null || "" == value) {
            sharedPreferences?.edit()?.remove(key)?.apply()
            return true
        }
        return try {
            persistInt(Integer.valueOf(value))
        } catch (e: NumberFormatException) {
            false
        }
    }

    override fun getPersistedString(defaultReturnValue: String?): String? {
        return if (sharedPreferences?.contains(key) ?: false) {
            try {
                val intValue = getPersistedInt(0)
                intValue.toString()
            } catch (e: ClassCastException) {
                Timber.e(
                    "Error retrieving string preference %s, returning default",
                    defaultReturnValue
                )
                defaultReturnValue
            }
        } else {
            defaultReturnValue
        }
    }
}