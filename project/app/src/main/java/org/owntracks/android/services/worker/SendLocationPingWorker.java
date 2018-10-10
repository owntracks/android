package org.owntracks.android.services.worker;

import android.content.Context;
import android.support.annotation.NonNull;

import org.owntracks.android.App;
import org.owntracks.android.services.BackgroundService;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import timber.log.Timber;

public class SendLocationPingWorker extends Worker {

    public SendLocationPingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Timber.v("SendLocationPingWorker doing work");
        App.getInstance().startBackgroundServiceCompat(getApplicationContext(), BackgroundService.INTENT_ACTION_SEND_LOCATION_PING);
        return Result.SUCCESS;
    }
}