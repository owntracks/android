package androidx.preference

import android.content.Context
import android.text.InputType.TYPE_CLASS_TEXT
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.util.TypedValue.TYPE_INT_DEC
import android.util.TypedValue.TYPE_INT_HEX
import androidx.core.util.forEach

class ValidatingEditTextPreference : EditTextPreference {
    private var onBindEditTextListener: OnBindEditTextListener? = null

    private val attributes = SparseArray<TypedValue>()

    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {

        attrs?.run {
            IntRange(0, this.attributeCount).forEach {
                val resourceValue = getAttributeResourceValue(it, 0)
                when (val name = getAttributeNameResource(it)) {
                    android.R.attr.inputType -> attributes.put(
                        name,
                        TypedValue().apply {
                            resourceId = resourceValue
                            data = getAttributeIntValue(it, TYPE_CLASS_TEXT)
                            type = TYPE_INT_HEX
                        }
                    )
                    android.R.attr.lines -> attributes.put(
                        name,
                        TypedValue().apply {
                            resourceId = resourceValue
                            data = getAttributeIntValue(it, -1)
                            type = TYPE_INT_DEC
                        }
                    )
                }
            }
        }
        super.setOnBindEditTextListener { editText ->
            attributes.forEach { attribute, value ->
                when (attribute) {
                    android.R.attr.inputType -> editText.inputType = value.data
                    android.R.attr.lines -> editText.setLines(value.data)
                }
            }
            onBindEditTextListener?.onBindEditText(editText)
        }
    }

    override fun getOnBindEditTextListener(): OnBindEditTextListener? {
        return super.getOnBindEditTextListener()
    }

    override fun setOnBindEditTextListener(onBindEditTextListener: OnBindEditTextListener?) {
        this.onBindEditTextListener = onBindEditTextListener
    }

    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    @Suppress("unused")
    constructor(context: Context) : super(context)
}
