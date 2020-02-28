package org.owntracks.android.services.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.owntracks.android.injection.components.AppComponentProvider;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.services.LocationProcessor;

import javax.inject.Inject;

import timber.log.Timber;

public class SendLocationPingWorker extends Worker {
    @Inject
    LocationProcessor locationProcessor;

    public SendLocationPingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        AppComponentProvider.getAppComponent().inject(this);
    }

    @NonNull
    @Override
    public Result doWork() {
        Timber.tag("outgoing").v("SendLocationPingWorker doing work. ThreadID: %s", Thread.currentThread());
        locationProcessor.publishLocationMessage(MessageLocation.REPORT_TYPE_PING);
        return Result.success();
    }
}