package org.owntracks.android.support;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.owntracks.android.services.ServiceProxy;

public class BluetoothStateChangeReceiver extends BroadcastReceiver{
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);

            ServiceProxy.runOrBind(context, new Runnable() {
                @Override
                public void run() {
                    ServiceProxy.getServiceBeacon().setBluetoothMode(state);
                }
            });
        }
    }
}
