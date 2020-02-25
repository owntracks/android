package org.owntracks.android.services.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.owntracks.android.injection.components.AppComponentProvider;
import org.owntracks.android.services.MessageProcessor;

import javax.inject.Inject;

import timber.log.Timber;

public class MQTTReconnectWorker extends Worker {
    @Inject
    MessageProcessor messageProcessor;

    public MQTTReconnectWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        AppComponentProvider.getAppComponent().inject(this);
    }


    @NonNull
    @Override
    public Result doWork() {
        Timber.tag("outgoing").i("MQTTReconnectWorker Doing work. ThreadID: %s", Thread.currentThread().getId());
        if(!messageProcessor.isEndpointConfigurationComplete())
            return Result.failure();
        return messageProcessor.statefulCheckConnection() ? Result.success() : Result.retry();
    }
}