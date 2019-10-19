package org.owntracks.android.support.receiver;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import org.owntracks.android.support.Preferences;

import javax.inject.Inject;

import dagger.android.DaggerBroadcastReceiver;
import timber.log.Timber;

public class WifiStateReceiver extends DaggerBroadcastReceiver {
	@Inject
	Preferences preferences;

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context,intent);
		Timber.v("connectivity broadcast intent received ");


		if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())){
			Timber.e("received wrong intent action");
			return;
		}

		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if(cm==null) {
			Timber.e("ConnectivityManager not available");
			return;
		}

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (null != activeNetwork) {
			Timber.v("state:%s", activeNetwork.getState());


			if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
				Timber.e("connectivity: ConnectivityManager.TYPE_WIFI");

			if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
				Timber.e("connectivity: ConnectivityManager.TYPE_MOBILE");
		} else {
			Timber.e("connectivity: ConnectivityManager.TYPE_NOT_CONNECTED");
		}


	}

}
