package org.owntracks.android.services.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.services.LocationProcessor
import timber.log.Timber
import javax.inject.Inject

@HiltWorker
class SendLocationPingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationProcessor: LocationProcessor
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        Timber.d("SendLocationPingWorker started. ThreadID: %s", Thread.currentThread())
        locationProcessor.publishLocationMessage(MessageLocation.REPORT_TYPE_PING)
        return Result.success()
    }

    class Factory @Inject constructor(private val locationProcessor: LocationProcessor) :
        ChildWorkerFactory {
        override fun create(appContext: Context, params: WorkerParameters): ListenableWorker =
            SendLocationPingWorker(appContext, params, locationProcessor)
    }
}