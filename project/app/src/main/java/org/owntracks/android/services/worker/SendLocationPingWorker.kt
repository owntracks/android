package org.owntracks.android.services.worker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Inject
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.services.LocationProcessor
import timber.log.Timber

@HiltWorker
class SendLocationPingWorker
@AssistedInject
constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val preferences: Preferences,
    private val locationProcessor: LocationProcessor
) : CoroutineWorker(context, workerParams) {

    private var service: BackgroundService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Timber.d("${this@SendLocationPingWorker::class.simpleName} has connected to $name")
            this@SendLocationPingWorker.service = (service as BackgroundService.LocalBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Timber.w("${this@SendLocationPingWorker::class.simpleName} has disconnected from $name")
            service = null
        }
    }

    init {
        context.bindService(
            Intent(context, BackgroundService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override suspend fun doWork(): Result {
        Timber.d("SendLocationPingWorker started")
        if (preferences.experimentalFeatures.contains(
                Preferences.EXPERIMENTAL_FEATURE_LOCATION_PING_USES_HIGH_ACCURACY_LOCATION_REQUEST
            )
        ) {
            service?.requestOnDemandLocationUpdate(MessageLocation.ReportType.PING) ?: run {
                Timber.w("No service bound, unable to ping location")
            }
        } else {
            locationProcessor.publishLocationMessage(MessageLocation.ReportType.PING)
        }
        return Result.success()
    }

    class Factory
    @Inject
    constructor(
        private val preferences: Preferences,
        private val locationProcessor: LocationProcessor
    ) : ChildWorkerFactory {
        override fun create(appContext: Context, params: WorkerParameters): ListenableWorker =
            SendLocationPingWorker(
                appContext,
                params,
                preferences,
                locationProcessor
            )
    }
}
