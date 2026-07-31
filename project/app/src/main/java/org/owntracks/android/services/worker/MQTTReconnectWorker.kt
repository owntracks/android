package org.owntracks.android.services.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Inject
import org.owntracks.android.services.MessageProcessor
import timber.log.Timber

@HiltWorker
class MQTTReconnectWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val messageProcessor: MessageProcessor,
    private val scheduler: Scheduler
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
    if (!messageProcessor.isEndpointReady) {
      // The endpoint can become ready later — the user finishes configuring it, or a mode change
      // completes — so keep asking rather than giving up permanently.
      Timber.d("Unable to reconnect as endpoint is not ready")
      scheduler.scheduleMqttReconnect()
      return Result.success()
    }
    if (messageProcessor.reconnect().isFailure) {
      // The endpoint's own connect-failure path also schedules a retry. Doing it here as well is
      // harmless: the work is unique, so at most one attempt is ever pending, and the only effect
      // of counting the failure twice is reaching the delay ceiling slightly sooner.
      Timber.d("Reconnect attempt failed, scheduling another")
      scheduler.scheduleMqttReconnect()
    }
    return Result.success().also { Timber.i("MQTT reconnect worker job completed, status $it") }
  }

  class Factory
  @Inject
  constructor(
      private val messageProcessor: MessageProcessor,
      private val scheduler: Scheduler
  ) : ChildWorkerFactory {
    override fun create(appContext: Context, params: WorkerParameters): ListenableWorker =
        MQTTReconnectWorker(appContext, params, messageProcessor, scheduler)
  }
}
