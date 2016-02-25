package org.owntracks.android.support.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.owntracks.android.services.ServiceProxy;

public class ReceiverProxy extends BroadcastReceiver{
    private static final String TAG = "ReceiverProxy";

    @Override
    public void onReceive(Context context, final Intent intent) {
        intent.setClass(context, ServiceProxy.class);
        context.startService(intent);
    }
}
