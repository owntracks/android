package org.owntracks.android.ui.preferences

import android.content.DialogInterface
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import androidx.preference.ValidatingEditTextPreference

class ValidatingEditTextPreferenceDialogFragmentCompat(
    private val preference: ValidatingEditTextPreference
) : EditTextPreferenceDialogFragmentCompat() {
  private var editText: EditText? = null

  private fun isValid(): Boolean {
    return editText?.run { preference.validationFunction(this.text.toString()) } ?: false
  }

  override fun onStart() {
    super.onStart()
    editText = dialog?.findViewById(android.R.id.edit)
    (this.dialog as AlertDialog?)?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
      if (isValid()) {
        super.onClick(dialog as DialogInterface, AlertDialog.BUTTON_POSITIVE)
        dismiss()
      } else {
        editText?.error =
            getString(preference.getValidationErrorMessage(), preference.validationErrorArgs)
      }
    }
  }
}
