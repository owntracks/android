package org.owntracks.android.services.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.services.MessageProcessor
import timber.log.Timber

/**
 * Periodically confirms the endpoint really is connected, and reconnects it if it isn't.
 *
 * Every other reconnect trigger in the app is reactive: a connectivity callback, a dropped
 * connection, a failed publish. Anything they collectively miss is therefore missed permanently,
 * which is how a device ends up queueing messages for eleven hours without a single reconnect
 * attempt (issue #2294). This is the one unconditional check.
 */
@HiltWorker
class MQTTConnectionWatchdogWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val messageProcessor: MessageProcessor,
    private val endpointStateRepo: EndpointStateRepo,
    private val preferences: Preferences,
) : CoroutineWorker(context, workerParams) {

  override suspend fun doWork(): Result {
    if (preferences.mode != ConnectionMode.MQTT) {
      // Only MQTT holds a connection to watch. The check is on the mode rather than on whether an
      // endpoint has been loaded: in a process WorkManager has just started for this job, nothing
      // has loaded one yet, and treating that as "nothing to check" is how the backstop came to do
      // nothing in precisely the situation it exists for.
      Timber.d("Connection watchdog: not in MQTT mode, nothing to check")
      return Result.success()
    }
    val state = endpointStateRepo.endpointState.value
    // Only worth spending a broker round-trip when we believe we are up; in any other state we
    // already know we are not, and the answer would not change what happens next.
    val connectionCheckPassed =
        state == EndpointState.CONNECTED && messageProcessor.checkConnection()
    if (!shouldReconnect(state, connectionCheckPassed)) {
      Timber.i("Connection watchdog: endpoint healthy")
      return Result.success()
    }
    // Logged at WARN deliberately: this firing at all means every reactive path missed something,
    // and that is exactly what is worth having in an exported log.
    Timber.w("Connection watchdog: endpoint unhealthy in state $state, reconnecting")
    // Binds the background service and builds the endpoint first if this process has neither, which
    // is the case whenever this job is what started it.
    messageProcessor.initializeAndReconnect()
    return Result.success()
  }

  companion object {
    /**
     * Whether an endpoint in this state, with this connection check result, is worth reconnecting.
     * Shared with [MQTTReconnectWorker]: both ask the same question, "is this connection actually
     * fine", just prompted by different triggers.
     *
     * Extracted so the policy can be tested without a [CoroutineWorker]'s Android dependencies.
     *
     * Note that [EndpointState.CONNECTING] is treated as unhealthy, unlike in the connectivity
     * callback where an in-flight attempt is left alone: on the watchdog's fifteen-minute cadence a
     * connect attempt still outstanding is stuck rather than progressing. [MQTTReconnectWorker]
     * inherits that same call for a state it can also see much sooner after a disconnect, where a
     * CONNECTING it finds is more likely still genuinely in progress — unchanged from its
     * pre-existing behaviour of reconnecting regardless of state, not a guarantee this policy was
     * built to make.
     */
    internal fun shouldReconnect(state: EndpointState, connectionCheckPassed: Boolean): Boolean =
        state != EndpointState.CONNECTED || !connectionCheckPassed
  }
}
