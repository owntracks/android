package org.owntracks.android.services.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.owntracks.android.services.MessageProcessor
import timber.log.Timber
import javax.inject.Inject

class MQTTMaybeReconnectAndPingWorker @Inject constructor(context: Context, workerParams: WorkerParameters, private val messageProcessor: MessageProcessor) : Worker(context, workerParams) {
    override fun doWork(): Result {
        Timber.d("MQTTMaybeReconnectAndPingWorker doing work on threadID: %s", Thread.currentThread())
        if (!messageProcessor.isEndpointConfigurationComplete) return Result.failure()
        return if (messageProcessor.statefulReconnectAndSendKeepalive()) Result.success() else Result.retry()
    }

    class Factory @Inject constructor(private val messageProcessor: MessageProcessor) : ChildWorkerFactory {
        override fun create(appContext: Context, params: WorkerParameters): ListenableWorker = MQTTMaybeReconnectAndPingWorker(appContext, params, messageProcessor)
    }
}

