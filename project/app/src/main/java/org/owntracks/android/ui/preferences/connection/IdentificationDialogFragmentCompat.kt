package org.owntracks.android.ui.preferences.connection

import android.view.View
import android.widget.EditText
import com.rengwuxian.materialedittext.MaterialEditText
import org.owntracks.android.R

class IdentificationDialogFragmentCompat constructor(
        key: String,
        private val model: Model,
        private val positiveCallback: (Model) -> Unit
) :
        PreferenceDialogFragmentCompatWithKey(key) {
    data class Model(
            internal val username: String,
            internal val password: String,
            internal val deviceId: String,
            internal val trackerId: String
    )

    private var usernameField: EditText? = null
    private var passwordField: EditText? = null
    private var deviceIdField: EditText? = null
    private var trackerIdField: EditText? = null
    override val validatedFields: List<MaterialEditText?> = emptyList()

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)
        usernameField =
                view?.findViewById<EditText>(R.id.username)?.apply { setText(model.username) }
        passwordField =
                view?.findViewById<EditText>(R.id.password)?.apply { setText(model.password) }
        deviceIdField =
                view?.findViewById<EditText>(R.id.deviceId)?.apply { setText(model.deviceId) }
        trackerIdField =
                view?.findViewById<EditText>(R.id.trackerId)?.apply { setText(model.trackerId) }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            ifLet(
                    usernameField,
                    passwordField,
                    deviceIdField,
                    trackerIdField
            ) { (usernameField, passwordField, deviceIdField, trackerIdField) ->
                positiveCallback(
                        Model(
                                usernameField.text.toString(),
                                passwordField.text.toString(),
                                deviceIdField.text.toString(),
                                trackerIdField.text.toString()
                        )
                )
            }
        }
    }
}