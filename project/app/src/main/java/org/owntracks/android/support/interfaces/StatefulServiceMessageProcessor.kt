package org.owntracks.android.support.interfaces

interface StatefulServiceMessageProcessor {
  suspend fun reconnect(): Result<Unit>

  fun checkConnection(): Boolean
}
