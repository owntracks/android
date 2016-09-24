package org.owntracks.android.support.interfaces;

import android.content.Intent;

import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.Events;

public interface ProxyableService {
	void onCreate(ServiceProxy c);

	void onDestroy();

	void onStartCommand(Intent intent);

	@Subscribe
	void onEvent(Events.Dummy event);
}
