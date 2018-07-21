package org.owntracks.android.workers;

import android.support.annotation.NonNull;

import org.owntracks.android.services.MessageProcessorEndpointMqtt;

import androidx.work.Worker;
import timber.log.Timber;

public class MQTTReconnectWorker extends Worker {
    @NonNull
    @Override
    public WorkerResult doWork() {
        Timber.i("Somesortofworker Doing work");
        boolean result = MessageProcessorEndpointMqtt.getInstance().checkConnection();
        return result ? WorkerResult.SUCCESS : WorkerResult.RETRY;
    }
}
