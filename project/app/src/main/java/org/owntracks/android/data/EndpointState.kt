package org.owntracks.android.data

import android.content.Context
import org.eclipse.paho.client.mqttv3.MqttException
import java.util.*

enum class EndpointState {
    INITIAL, IDLE, CONNECTING, CONNECTED, DISCONNECTED, ERROR, ERROR_DATADISABLED, ERROR_CONFIGURATION;

    var message: String? = null
        get() {
            if (field == null) {
                return error?.let {
                    if (it is MqttException && it.cause != null) {
                        if (it.message != null && it.message == "MqttException") {
                            String.format("MQTT Error: %s", it.cause!!.message)
                        } else String.format("MQTT Error: %s", it.message)
                    } else error!!.message
                }
            } else if (error != null) {
                return String.format(Locale.ROOT, "%s: %s", field, error!!.message)
            }
            return field
        }
    var error: Throwable? = null
        private set

    fun withMessage(message: String?): EndpointState {
        this.message = message
        return this
    }

    fun getLabel(context: Context): String {
        val res = context.resources
        val resId = res.getIdentifier(name, "string", context.packageName)
        return if (0 != resId) {
            res.getString(resId)
        } else name
    }

    fun withError(error: Throwable): EndpointState {
        this.error = error
        return this
    }
}
