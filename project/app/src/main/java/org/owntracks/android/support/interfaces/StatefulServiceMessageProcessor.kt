package org.owntracks.android.support.interfaces

interface StatefulServiceMessageProcessor {
    fun reconnect()
    fun checkConnection(): Boolean
}