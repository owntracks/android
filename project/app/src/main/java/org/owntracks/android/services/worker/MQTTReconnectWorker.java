package org.owntracks.android.services.worker;

import android.support.annotation.NonNull;

import org.owntracks.android.services.MessageProcessorEndpointMqtt;

import androidx.work.Worker;
import timber.log.Timber;

public class MQTTReconnectWorker extends Worker {
    @NonNull
    @Override
    public Result doWork() {
        Timber.i("MQTTReconnectWorker Doing work");
        boolean result = MessageProcessorEndpointMqtt.getInstance().checkConnection();
        return result ? Result.SUCCESS : Result.RETRY;
    }
}