package org.owntracks.android.data

import android.content.Context
import org.owntracks.android.R

enum class EndpointState {
    INITIAL, IDLE, CONNECTING, CONNECTED, DISCONNECTED, ERROR, ERROR_DATADISABLED, ERROR_CONFIGURATION;

    var message: String? = null
    var error: Throwable? = null
        private set

    fun withMessage(message: String): EndpointState {
        this.message = message
        return this
    }

    fun getLabel(context: Context): String = when (this) {
        INITIAL -> context.resources.getString(R.string.INITIAL)
        IDLE -> context.resources.getString(R.string.IDLE)
        CONNECTING -> context.resources.getString(R.string.CONNECTING)
        CONNECTED -> context.resources.getString(R.string.CONNECTED)
        DISCONNECTED -> context.resources.getString(R.string.DISCONNECTED)
        ERROR -> context.resources.getString(R.string.ERROR)
        ERROR_DATADISABLED -> context.resources.getString(R.string.ERROR_DATADISABLED)
        ERROR_CONFIGURATION -> context.resources.getString(R.string.ERROR_CONFIGURATION)
    }

    fun withError(error: Throwable): EndpointState {
        this.error = error
        return this
    }

    override fun toString(): String {
        return if (message != null) {
            "${super.toString()} ($message)"
        } else {
            super.toString()
        }
    }
}
