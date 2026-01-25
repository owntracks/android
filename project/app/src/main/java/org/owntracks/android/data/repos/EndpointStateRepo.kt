package org.owntracks.android.data.repos

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import org.owntracks.android.data.EndpointState
import timber.log.Timber

@Singleton
class EndpointStateRepo @Inject constructor() {

  val endpointState: MutableStateFlow<EndpointState> = MutableStateFlow(EndpointState.IDLE)

  val currentEndpointHost: MutableStateFlow<String> = MutableStateFlow("")

  val endpointQueueLength: MutableStateFlow<Int> = MutableStateFlow(0)

  val serviceStartedDate: MutableStateFlow<Instant> = MutableStateFlow(Instant.now())

  val lastSuccessfulMessageTime: MutableStateFlow<Instant?> = MutableStateFlow(null)

  val nextReconnectTime: MutableStateFlow<Instant?> = MutableStateFlow(null)

  suspend fun setState(newEndpointState: EndpointState) {
    Timber.v(
        "Setting endpoint state $newEndpointState called from: ${
            Thread.currentThread().stackTrace[3].run {
                "$className: $methodName"
            }
            }")
    endpointState.emit(newEndpointState)
    // Clear next reconnect time when we start connecting or are connected
    if (newEndpointState == EndpointState.CONNECTING || newEndpointState == EndpointState.CONNECTED) {
      nextReconnectTime.emit(null)
    }
  }

  suspend fun setQueueLength(queueLength: Int) {
    Timber.v("Setting queuelength=$queueLength")
    endpointQueueLength.emit(queueLength)
  }

  suspend fun setServiceStartedNow() {
    serviceStartedDate.emit(Instant.now())
  }

  suspend fun setLastSuccessfulMessageTime(time: Instant) {
    Timber.v("Setting lastSuccessfulMessageTime=$time")
    lastSuccessfulMessageTime.emit(time)
  }

  suspend fun setNextReconnectTime(time: Instant?) {
    Timber.v("Setting nextReconnectTime=$time")
    nextReconnectTime.emit(time)
  }

  suspend fun setCurrentEndpointHost(host: String) {
    Timber.v("Setting currentEndpointHost=$host")
    currentEndpointHost.emit(host)
  }
}
