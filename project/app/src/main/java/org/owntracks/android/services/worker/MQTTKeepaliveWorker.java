package org.owntracks.android.services.worker;


import android.support.annotation.NonNull;

import org.owntracks.android.services.MessageProcessorEndpointMqtt;

import androidx.work.Worker;
import timber.log.Timber;

public class MQTTKeepaliveWorker extends Worker {
    @NonNull
    @Override
    public Result doWork() {
        Timber.v("MQTTKeepaliveWorker doing work");
        boolean result = MessageProcessorEndpointMqtt.getInstance().sendPing();
        return result ? Result.SUCCESS : Result.RETRY;
    }
}