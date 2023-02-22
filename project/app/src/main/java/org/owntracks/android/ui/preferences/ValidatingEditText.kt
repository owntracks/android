package org.owntracks.android.ui.preferences

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ValidatingEditText : TextInputEditText {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    var validationFunction: (String) -> Boolean = { _ -> true }
    var validationErrorMessage: String = ""

    fun validate(): Boolean {
        val parentLayout = this.parent.parent as TextInputLayout
        val valid = validationFunction(this.text.toString())
        parentLayout.apply {
            error = validationErrorMessage
            isErrorEnabled = !valid
        }
        return valid
    }
}
