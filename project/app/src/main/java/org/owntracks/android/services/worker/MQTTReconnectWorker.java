package org.owntracks.android.services.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.owntracks.android.injection.components.AppComponentProvider;
import org.owntracks.android.services.MessageProcessor;

import java.util.concurrent.Semaphore;

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
        Timber.tag("mqtt").i("MQTTReconnectWorker Doing work on threadID: %s", Thread.currentThread());
        if (!messageProcessor.isEndpointConfigurationComplete())
            return Result.failure();
        // We're going to try and call messagePrcessor.reconnect() here, which may reinvoke itself on
        // a different thread. One option here was to faff around with futures, but it seems easier just to
        // Create a semaphore, pass it to the method and then just wait for it to be released, no matter where from.
        Semaphore lock = new Semaphore(1);
        lock.acquireUninterruptibly();
        messageProcessor.reconnect(lock);
        try {
            lock.acquire();
            return messageProcessor.statefulCheckConnection() ? Result.success() : Result.retry();
        } catch (InterruptedException e) {
            return Result.failure();
        }
    }
}