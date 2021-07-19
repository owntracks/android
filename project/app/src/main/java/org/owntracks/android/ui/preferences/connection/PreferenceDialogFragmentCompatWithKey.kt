package org.owntracks.android.ui.preferences.connection

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceDialogFragmentCompat
import com.rengwuxian.materialedittext.MaterialEditText

abstract class PreferenceDialogFragmentCompatWithKey(private val key: String) :
        PreferenceDialogFragmentCompat() {
    init {
        arguments = Bundle(1).apply { putString(ARG_KEY, key) }
    }

    protected abstract val validatedFields: List<MaterialEditText?>

    override fun onStart() {
        super.onStart()
        (this.dialog as AlertDialog?)?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            if (validatedFields.all { it != null && it.validate() }) {
                super.onClick(dialog, AlertDialog.BUTTON_POSITIVE)
                dismiss()
            }
        }
    }
}