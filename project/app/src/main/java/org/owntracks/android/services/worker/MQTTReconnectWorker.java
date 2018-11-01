package org.owntracks.android.services.worker;

import android.content.Context;
import android.support.annotation.NonNull;

import org.owntracks.android.injection.components.AppComponentProvider;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.services.MessageProcessorEndpointMqtt;

import javax.inject.Inject;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import timber.log.Timber;

public class MQTTReconnectWorker extends Worker {
    @Inject
    MessageProcessor messageProcessor;

    public MQTTReconnectWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }


    @NonNull
    @Override
    public Result doWork() {
        Timber.i("MQTTReconnectWorker Doing work");
        return messageProcessor.statefulCheckConnection() ? Result.SUCCESS : Result.RETRY;
    }
}