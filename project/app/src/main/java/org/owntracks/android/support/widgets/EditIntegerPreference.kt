package org.owntracks.android.support.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ValidatingEditTextPreference
import timber.log.Timber

class EditIntegerPreference(context: Context, attrs: AttributeSet?) :
    ValidatingEditTextPreference(context, attrs) {
  override fun persistString(value: String?): Boolean {
    return try {
      persistInt(Integer.valueOf(value ?: "0"))
    } catch (e: NumberFormatException) {
      false
    }
  }

  override fun getPersistedString(defaultReturnValue: String?): String? {
    return try {
      getPersistedInt(0).toString()
    } catch (e: ClassCastException) {
      Timber.e("Error retrieving string preference $key, returning default")
      defaultReturnValue
    }
  }
}
