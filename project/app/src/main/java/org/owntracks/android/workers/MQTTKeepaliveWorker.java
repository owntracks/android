package org.owntracks.android.workers;

import android.support.annotation.NonNull;

import org.owntracks.android.services.MessageProcessorEndpointMqtt;

import androidx.work.Worker;
import timber.log.Timber;

public class MQTTKeepaliveWorker extends Worker {
    @NonNull
    @Override
    public WorkerResult doWork() {
        Timber.v("MQTTKeepaliveWorker doing work");
        boolean result = MessageProcessorEndpointMqtt.getInstance().sendPing();
        return result ? WorkerResult.SUCCESS : WorkerResult.RETRY;
    }
}