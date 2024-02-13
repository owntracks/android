package org.owntracks.android.net.http

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.owntracks.android.net.ConnectionConfiguration
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException

data class HttpConfiguration(val url: String, val username: String, val password: String, val deviceId: String) :
    ConnectionConfiguration {
    override fun validate() {
        try {
            url.toHttpUrl()
        } catch (e: IllegalArgumentException) {
            throw ConfigurationIncompleteException(e)
        }
    }
}
