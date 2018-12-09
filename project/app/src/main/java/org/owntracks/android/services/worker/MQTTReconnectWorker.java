package org.owntracks.android.services.worker;

import android.content.Context;
import android.support.annotation.NonNull;

import org.owntracks.android.injection.components.AppComponentProvider;
import org.owntracks.android.services.MessageProcessor;

import javax.inject.Inject;

import androidx.work.Result;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
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
        Timber.i("MQTTReconnectWorker Doing work (%s)",Thread.currentThread());
        return messageProcessor.statefulCheckConnection() ? Result.success() : Result.retry();
    }
}