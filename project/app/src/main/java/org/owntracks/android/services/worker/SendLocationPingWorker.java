package org.owntracks.android.services.worker;

import android.support.annotation.NonNull;

import org.owntracks.android.App;
import org.owntracks.android.services.BackgroundService;

import androidx.work.Worker;
import timber.log.Timber;

public class SendLocationPingWorker extends Worker {
    @NonNull
    @Override
    public Result doWork() {
        Timber.v("SendLocationPingWorker doing work");
        App.startBackgroundServiceCompat(getApplicationContext(), BackgroundService.INTENT_ACTION_SEND_LOCATION_PING);
        return Result.SUCCESS;
    }
}