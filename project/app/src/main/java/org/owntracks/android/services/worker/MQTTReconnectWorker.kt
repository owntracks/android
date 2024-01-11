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
class MQTTReconnectWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val messageProcessor: MessageProcessor
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        Timber.i("MQTT reconnect worker job started")
        if (!messageProcessor.isEndpointReady) {
            return Result.failure().also {
                Timber.w("Unable to reconnect as endpoint is not ready")
            }
        }
        return if (messageProcessor.reconnect().isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }.also { Timber.i("MQTT reconnect worker job completed, status $it") }
    }

    class Factory @Inject constructor(private val messageProcessor: MessageProcessor) : ChildWorkerFactory {
        override fun create(appContext: Context, params: WorkerParameters): ListenableWorker =
            MQTTReconnectWorker(appContext, params, messageProcessor)
    }
}
