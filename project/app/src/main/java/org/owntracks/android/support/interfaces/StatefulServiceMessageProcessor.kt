package org.owntracks.android.support.interfaces

import java.util.concurrent.CompletableFuture

interface StatefulServiceMessageProcessor {
    fun reconnect(): CompletableFuture<Unit>?
    fun checkConnection(): Boolean
}
