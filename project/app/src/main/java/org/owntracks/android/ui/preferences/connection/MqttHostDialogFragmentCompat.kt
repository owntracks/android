package org.owntracks.android.ui.preferences.connection

import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.SwitchCompat
import com.rengwuxian.materialedittext.MaterialEditText
import org.owntracks.android.R

class MqttHostDialogFragmentCompat constructor(
        key: String,
        private val model: Model,
        private val positiveCallback: (Model) -> Unit
) : PreferenceDialogFragmentCompatWithKey(key) {

    private var hostField: EditText? = null
    private var portField: EditText? = null
    private var clientIdField: EditText? = null
    private var websocketField: SwitchCompat? = null

    data class Model(
            internal val host: String,
            internal val port: Int?,
            internal val clientId: String,
            internal val webSockets: Boolean
    )

    override val validatedFields: List<MaterialEditText?> = emptyList()

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)
        hostField = view?.findViewById<EditText>(R.id.host)?.apply { setText(model.host) }
        model.port?.also {
            portField = view?.findViewById<EditText>(R.id.port)?.apply { setText(it.toString()) }
        }
        clientIdField =
                view?.findViewById<EditText>(R.id.clientId)?.apply { setText(model.clientId) }
        websocketField =
                view?.findViewById<SwitchCompat>(R.id.ws)?.apply { isChecked = model.webSockets }

    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            ifLet(
                    hostField,
                    portField,
                    clientIdField,
                    websocketField
            ) { (hostField, portField, clientIdField, websocketField) ->

                positiveCallback(
                        Model(
                                host = hostField.text.toString(),
                                port = portField.text.toString().toIntOrNull(),
                                clientId = clientIdField.text.toString(),
                                webSockets = (websocketField as SwitchCompat).isChecked
                        )
                )
            }
        }
    }
}