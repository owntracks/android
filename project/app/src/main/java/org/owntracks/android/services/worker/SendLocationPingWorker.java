package org.owntracks.android.services.worker;

import android.content.Context;
import android.support.annotation.NonNull;

import org.owntracks.android.App;
import org.owntracks.android.data.repos.LocationRepo;
import org.owntracks.android.injection.components.AppComponentProvider;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.services.BackgroundService;
import org.owntracks.android.services.LocationProcessor;

import javax.inject.Inject;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
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
        Timber.v("SendLocationPingWorker doing work");
        locationProcessor.publishLocationMessage(MessageLocation.REPORT_TYPE_PING);
        return Result.SUCCESS;
    }
}