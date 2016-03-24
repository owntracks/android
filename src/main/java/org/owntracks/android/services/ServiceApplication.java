package org.owntracks.android.services;

import org.owntracks.android.App;
import org.owntracks.android.messages.MessageConfiguration;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.MessageWaypointCollection;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.StaticHandlerInterface;

import android.content.Intent;
import android.os.Message;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

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
    public void onStartCommand(Intent intent, int flags, int startId) {
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
        return result == ConnectionResult.SUCCESS;

    }

    public boolean publishWaypointsMessage() {
        MessageWaypoints m = new MessageWaypoints();
        MessageWaypointCollection waypoints = Preferences.waypointsToJSON();
        if(waypoints == null)
            return false;

        m.setWaypoints(waypoints);

        ServiceProxy.getServiceMessage().sendMessage(m);
        return true;
    }
}
