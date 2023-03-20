package org.owntracks.android.support.interfaces

import java.util.concurrent.CompletableFuture

interface StatefulServiceMessageProcessor {
    suspend fun reconnect(): Boolean
    fun checkConnection(): Boolean
}
