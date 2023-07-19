package org.owntracks.android.support.interfaces

interface StatefulServiceMessageProcessor {
    suspend fun reconnect(): Boolean
    fun checkConnection(): Boolean
}
