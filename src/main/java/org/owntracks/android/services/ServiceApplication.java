package org.owntracks.android.services;

import org.owntracks.android.App;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.StaticHandlerInterface;

import android.content.Intent;
import android.os.Message;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.List;

public class ServiceApplication implements ProxyableService, StaticHandlerInterface {
    private static final String TAG = "ServiceApplication";

    private static boolean playServicesAvailable;
    private ServiceProxy context;
    @Override
    public void onCreate(ServiceProxy context) {
        this.context = context;
        checkPlayServices();
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return 0;
    }

    @Override
    public void onEvent(Events.Dummy event) {

    }


    @Override
    public void handleHandlerMessage(Message msg) {
    }

	public static boolean checkPlayServices() {
            GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
            int result = googleAPI.isGooglePlayServicesAvailable(App.getContext());
            if(result != ConnectionResult.SUCCESS) {
                return false;
            }

            return true;

	}
}
