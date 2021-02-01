package org.owntracks.android.services.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.owntracks.android.injection.components.AppComponentProvider
import org.owntracks.android.services.MessageProcessor
import timber.log.Timber
import javax.inject.Inject

class MQTTMaybeReconnectAndPingWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    @JvmField
    @Inject
    var messageProcessor: MessageProcessor? = null
    override fun doWork(): Result {
        Timber.tag("MQTT").d("MQTTMaybeReconnectAndPingWorker doing work on threadID: %s", Thread.currentThread())
        if (!messageProcessor!!.isEndpointConfigurationComplete) return Result.failure()
        return if (messageProcessor!!.statefulReconnectAndSendKeepalive()) Result.success() else Result.retry()
    }

    init {
        AppComponentProvider.getAppComponent().inject(this)
    }
}