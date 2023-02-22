package org.owntracks.android.ui.preferences

import android.content.DialogInterface
import android.text.InputFilter
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreferenceDialogFragmentCompat

open class ValidatingEditTextPreferenceDialogFragmentCompat(
    @StringRes private val errorMessage: Int,
    private val errorArgs: Any? = null,
    private val maxLength: Int = -1,
    private val validationFunction: (String) -> Boolean = { _ -> true }
) : EditTextPreferenceDialogFragmentCompat() {
    private var editText: EditText? = null
    private fun isValid(): Boolean {
        return editText?.run { validationFunction(this.text.toString()) } ?: false
    }

    override fun onStart() {
        super.onStart()
        editText = dialog?.findViewById(android.R.id.edit)
        if (maxLength >= 0) {
            editText?.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        }
        (this.dialog as AlertDialog?)?.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setOnClickListener {
                if (isValid()) {
                    super.onClick(dialog as DialogInterface, AlertDialog.BUTTON_POSITIVE)
                    dismiss()
                } else {
                    editText?.error = getString(errorMessage, errorArgs)
                }
            }
    }
}
