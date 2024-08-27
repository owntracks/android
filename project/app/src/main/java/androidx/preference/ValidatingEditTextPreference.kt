package androidx.preference

import android.content.Context
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.Spanned
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.util.TypedValue.TYPE_INT_DEC
import android.util.TypedValue.TYPE_INT_HEX
import android.util.TypedValue.TYPE_REFERENCE
import android.util.TypedValue.TYPE_STRING
import androidx.annotation.StringRes
import androidx.core.util.forEach

open class ValidatingEditTextPreference : EditTextPreference {
  private var onBindEditTextListener: OnBindEditTextListener? = null

  private val attributes = SparseArray<TypedValue>()

  var validationFunction: (String) -> Boolean = { _ -> true }

  var validationErrorArgs: Any? = null

  constructor(
      context: Context,
      attrs: AttributeSet?,
      defStyleAttr: Int,
      defStyleRes: Int
  ) : super(context, attrs, defStyleAttr, defStyleRes) {

    attrs?.run {
      IntRange(0, this.attributeCount).forEach {
        val resourceValue = getAttributeResourceValue(it, 0)
        when (val name = getAttributeNameResource(it)) {
          android.R.attr.inputType ->
              attributes.put(
                  name,
                  TypedValue().apply {
                    resourceId = resourceValue
                    data = getAttributeIntValue(it, TYPE_CLASS_TEXT)
                    type = TYPE_INT_HEX
                  })
          android.R.attr.lines ->
              attributes.put(
                  name,
                  TypedValue().apply {
                    resourceId = resourceValue
                    data = getAttributeIntValue(it, -1)
                    type = TYPE_INT_DEC
                  })
          android.R.attr.digits ->
              attributes.put(
                  name,
                  TypedValue().apply {
                    resourceId = resourceValue
                    string = getAttributeValue(it)
                    type = TYPE_STRING
                  })
          android.R.attr.maxLength ->
              attributes.put(
                  name,
                  TypedValue().apply {
                    resourceId = resourceValue
                    data = getAttributeIntValue(it, -1)
                    type = TYPE_INT_DEC
                  })
          org.owntracks.android.R.attr.validationError -> {
            attributes.put(
                name,
                TypedValue().apply {
                  resourceId = resourceValue
                  data = getAttributeResourceValue(it, TYPE_CLASS_TEXT)
                  type = TYPE_REFERENCE
                })
          }
        }
      }
    }
    super.setOnBindEditTextListener { editText ->
      attributes.forEach { attribute, value ->
        when (attribute) {
          android.R.attr.inputType -> editText.inputType = value.data
          android.R.attr.lines -> editText.setLines(value.data)
          android.R.attr.maxLength ->
              editText.filters =
                  editText.filters
                      .toMutableList()
                      .apply { add(LengthFilter(value.data)) }
                      .toTypedArray()
          android.R.attr.digits ->
              editText.filters =
                  editText.filters
                      .toMutableList()
                      .apply { add(SpecificCharsInputFilter(value.string.toList())) }
                      .toTypedArray()
        }
      }
      onBindEditTextListener?.onBindEditText(editText)
    }
  }

  /**
   * An [InputFilter] that only allows characters in the supplied list to be input
   *
   * @property chars list of permitted [Char]
   * @constructor Create empty Specific chars input filter
   */
  class SpecificCharsInputFilter(private val chars: List<Char>) : InputFilter {
    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence {
      return source?.toList()?.filter { chars.contains(it) }?.joinToString("") ?: ""
    }
  }

  override fun getOnBindEditTextListener(): OnBindEditTextListener? {
    return super.getOnBindEditTextListener()
  }

  override fun setOnBindEditTextListener(onBindEditTextListener: OnBindEditTextListener?) {
    this.onBindEditTextListener = onBindEditTextListener
  }

  @StringRes
  fun getValidationErrorMessage(): Int {
    return attributes[org.owntracks.android.R.attr.validationError].data
  }

  constructor(
      context: Context,
      attrs: AttributeSet?,
      defStyleAttr: Int
  ) : this(context, attrs, defStyleAttr, 0)

  constructor(
      context: Context,
      attrs: AttributeSet?
  ) : this(context, attrs, R.attr.editTextPreferenceStyle)

  @Suppress("unused") constructor(context: Context) : this(context, null)
}
