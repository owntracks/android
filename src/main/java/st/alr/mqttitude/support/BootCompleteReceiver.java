package st.alr.mqttitude.support;

import st.alr.mqttitude.services.ServiceProxy;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompleteReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(this.toString(), "BootCompleteReceiver received intent");

		if ((intent.getAction() != null)
				&& (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
				&& Preferences.getAutostartOnBoot()) {
			Log.v(this.toString(), "Autostarting app");
			Intent i = new Intent(context, ServiceProxy.class);
			context.startService(i);
		}

	}

}
