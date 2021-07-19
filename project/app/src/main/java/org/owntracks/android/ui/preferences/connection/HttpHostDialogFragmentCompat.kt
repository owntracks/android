package org.owntracks.android.ui.preferences.connection

import android.view.View
import android.widget.EditText
import com.rengwuxian.materialedittext.MaterialEditText
import org.owntracks.android.R

class HttpHostDialogFragmentCompat constructor(
        key: String,
        private val model: Model,
        private val positiveCallback: (Model) -> Unit
) :
        PreferenceDialogFragmentCompatWithKey(key) {
    data class Model(internal val url: String)

    private var urlField: EditText? = null
    override val validatedFields: List<MaterialEditText?> = emptyList()

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)
        urlField = view?.findViewById<EditText>(R.id.url)?.apply { setText(model.url) }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            urlField?.run { positiveCallback(Model(this.text.toString())) }
        }
    }
}

