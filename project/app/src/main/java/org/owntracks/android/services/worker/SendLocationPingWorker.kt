package org.owntracks.android.services.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Inject
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.services.LocationProcessor
import timber.log.Timber

@HiltWorker
class SendLocationPingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationProcessor: LocationProcessor
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
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
