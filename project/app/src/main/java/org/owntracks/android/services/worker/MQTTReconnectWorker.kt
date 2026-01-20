package org.owntracks.android.services.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkRequest.Companion.MAX_BACKOFF_MILLIS
import androidx.work.WorkRequest.Companion.MIN_BACKOFF_MILLIS
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.pow
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
    private val endpointStateRepo: EndpointStateRepo
) : CoroutineWorker(context, workerParams) {
  override suspend fun doWork(): Result {
    Timber.i("MQTT reconnect worker job started (attempt $runAttemptCount)")
    if (!messageProcessor.isEndpointReady) {
      return Result.failure().also { Timber.w("Unable to reconnect as endpoint is not ready") }
    }
    return if (messageProcessor.reconnect().isSuccess) {
          Result.success()
        } else {
          // Calculate the backoff delay for the next retry
          // WorkManager uses exponential backoff: MIN_BACKOFF * 2^attempt, capped at MAX_BACKOFF
          // Use maxOf to ensure we never have a negative exponent on the first attempt
          val backoffDelayMs = min(
              MIN_BACKOFF_MILLIS * 2.0.pow(maxOf(0, runAttemptCount)).toLong(),
              MAX_BACKOFF_MILLIS
          )
          val nextReconnectTime = Instant.now().plusMillis(backoffDelayMs)
          Timber.d("Next reconnect attempt in ${backoffDelayMs}ms at $nextReconnectTime")
          endpointStateRepo.setNextReconnectTime(nextReconnectTime)
          Result.retry()
        }
        .also { Timber.i("MQTT reconnect worker job completed, status $it") }
  }

  class Factory @Inject constructor(
      private val messageProcessor: MessageProcessor,
      private val endpointStateRepo: EndpointStateRepo
  ) : ChildWorkerFactory {
    override fun create(appContext: Context, params: WorkerParameters): ListenableWorker =
        MQTTReconnectWorker(appContext, params, messageProcessor, endpointStateRepo)
  }
}
