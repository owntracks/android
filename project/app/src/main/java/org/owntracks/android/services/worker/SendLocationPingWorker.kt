package org.owntracks.android.services.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.owntracks.android.injection.components.AppComponentProvider
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.services.LocationProcessor
import timber.log.Timber
import javax.inject.Inject

class SendLocationPingWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    @JvmField
    @Inject
    var locationProcessor: LocationProcessor? = null
    override fun doWork(): Result {
        Timber.tag("MQTT").d("SendLocationPingWorker doing work. ThreadID: %s", Thread.currentThread())
        locationProcessor!!.publishLocationMessage(MessageLocation.REPORT_TYPE_PING)
        return Result.success()
    }

    init {
        AppComponentProvider.getAppComponent().inject(this)
    }
}