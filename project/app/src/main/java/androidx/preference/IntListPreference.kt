package androidx.preference

import android.content.Context
import android.util.AttributeSet

class IntListPreference : ListPreference {
  @Suppress("unused")
  constructor(
      context: Context,
      attrs: AttributeSet?,
      defStyleAttr: Int,
      defStyleRes: Int
  ) : super(context, attrs, defStyleAttr, defStyleRes)

  @Suppress("unused")
  constructor(
      context: Context,
      attrs: AttributeSet?,
      defStyleAttr: Int
  ) : super(context, attrs, defStyleAttr)

  @Suppress("unused") constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

  @Suppress("unused") constructor(context: Context) : super(context)

  override fun getPersistedString(defaultReturnValue: String?): String {
    defaultReturnValue?.also {
      return getPersistedInt(defaultReturnValue.toInt()).toString()
    }
    return getPersistedInt(0).toString()
  }

  override fun persistString(value: String?): Boolean {
    return persistInt(value!!.toInt())
  }
}
