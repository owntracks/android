package org.owntracks.android.support.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import timber.log.Timber;

public final class TaskerReceiver extends BroadcastReceiver
{


    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        Timber.v("Receiver Fired");
    }
}
