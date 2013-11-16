package st.alr.mqttitude.support;

import st.alr.mqttitude.services.ServiceLocator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BackgroundPublishReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context arg0, Intent arg1) {
        if (arg1.getAction().equals(Defaults.INTENT_ACTION_PUBLISH_LASTKNOWN)) {
            ServiceLocator s = ServiceLocator.getInstance();
            if(s != null)
                s.publishLastKnownLocation();
            else
                Log.e(this.toString(), "No service connection, aborting pub from notification");
        }
    }

}

