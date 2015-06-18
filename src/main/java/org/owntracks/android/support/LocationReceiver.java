package org.owntracks.android.support;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.owntracks.android.services.ServiceProxy;

public class LocationReceiver extends BroadcastReceiver {
    Context context;

    @Override
    public void onReceive(Context context, final Intent intent) {
        if (intent.getAction().equals("org.owntracks.android.geofence.ACTION_RECEIVE_GEOFENCE")) {
            Log.v(this.toString(), "INTENT_ACTION_FENCE_TRANSITION received");

            ServiceProxy.runOrBind(context, new Runnable() {
                @Override
                public void run() {
                    Log.v(this.toString(), "delivering fence transition event to service locator");
                    ServiceProxy.getServiceLocator().onStartCommand(intent, 0, 0);
                }
            });
        }

    }
}


