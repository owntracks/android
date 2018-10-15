package org.owntracks.android.support;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;

import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.messages.MessageLocation;

import javax.annotation.Nullable;
import javax.inject.Inject;

@PerApplication
public class DeviceMetricsProvider {

    private final Context context;

    @Inject
    DeviceMetricsProvider(@AppContext Context context) {
        this.context = context;
    }

    public int getBatteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            return batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
       }
        return 0;
    }

    @Nullable
    public String getConnectionType() {
        ConnectivityManager cm = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        NetworkInfo activeNetwork;
        if (cm != null && (activeNetwork = cm.getActiveNetworkInfo()) != null) {
            if (!activeNetwork.isConnected()) {
                return MessageLocation.CONN_TYPE_OFFLINE;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return MessageLocation.CONN_TYPE_WIFI;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return MessageLocation.CONN_TYPE_MOBILE;
            }
        }
        return null;
    }

}
