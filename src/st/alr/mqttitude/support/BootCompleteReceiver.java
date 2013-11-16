
package st.alr.mqttitude.support;

import st.alr.mqttitude.services.ServiceApplication;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

public class BootCompleteReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ((intent.getAction() != null)
                && (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
                && (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                        "autostartOnBoot", false)))
        {

            Intent i = new Intent(context, ServiceApplication.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(i);
        }

    }

}
