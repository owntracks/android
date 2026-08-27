package org.owntracks.android.services.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.services.MessageProcessor
import timber.log.Timber

@HiltWorker
class MQTTReconnectWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val messageProcessor: MessageProcessor,
    private val scheduler: Scheduler,
    private val endpointStateRepo: EndpointStateRepo
) : CoroutineWorker(context, workerParams) {
  /**
   * Always reports [Result.success], because whether another attempt is needed is not WorkManager's
   * decision to make here.
   *
   * [Result.retry] would layer WorkManager's own backoff — capped at five hours — on top of the
   * bounded one [Scheduler.scheduleMqttReconnect] applies, and [Result.failure] is terminal, which
   * for a reconnect means never trying again for the life of the process. Instead every path that
   * still wants a reconnect schedules one explicitly.
   */
  override suspend fun doWork(): Result {
    Timber.i("MQTT reconnect worker job started")
    /*
    Nothing cancels this job when a different, faster path — most commonly the connectivity
    callback's immediate reconnect on a network becoming available — restores the connection before
    this scheduled attempt runs: WorkManager offers no way to cancel a job's own unique work from
    inside itself without risking cancelling this very execution mid-flight (a real hazard here,
    since a cancellation landing during the subscribe call a successful connect makes right
    afterwards would be caught by connectToBroker's own catch block and misreported as a failure).
    So this checks whether the connection already looks healthy before touching it at all, rather
    than assuming a scheduled reconnect is still needed just because it was once scheduled. Reuses
    the watchdog's policy: the two ask the same question, "is this connection actually fine",
    just prompted by different triggers.
     */
    val state = endpointStateRepo.endpointState.value
    val connectionCheckPassed = state == EndpointState.CONNECTED && messageProcessor.checkConnection()
    if (!MQTTConnectionWatchdogWorker.shouldReconnect(state, connectionCheckPassed)) {
      Timber.d("Connection already healthy, nothing to do")
      return Result.success()
    }
    // initializeAndReconnect rather than reconnect, because WorkManager may well have started this
    // process to run this very job, in which case nothing has bound the background service or built
    // the endpoint yet. Guarding on MessageProcessor.isEndpointReady here instead — as this used to
    // — is worse than useless: an endpoint that has not been loaded reports "not ready" no matter
    // how complete its configuration is, so a reconnect after a process death rescheduled itself
    // forever without ever attempting a connection.
    if (messageProcessor.initializeAndReconnect().isFailure) {
      // Covers an incomplete configuration as well as a failed connection: the endpoint can become
      // configured later — the user finishes setting it up, or a mode change completes — so keep
      // asking rather than giving up permanently.
      //
      // The endpoint's own connect-failure path also schedules a retry. Doing it here as well is
      // harmless: the work is unique, so at most one attempt is ever pending, and the only effect
      // of counting the failure twice is reaching the delay ceiling slightly sooner.
      Timber.d("Reconnect attempt failed, scheduling another")
      scheduler.scheduleMqttReconnect()
    }
    return Result.success().also { Timber.i("MQTT reconnect worker job completed, status $it") }
  }
}
