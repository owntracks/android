package org.owntracks.android.services;

import android.content.Intent;

import org.owntracks.android.support.Events;

public interface ProxyableService {
	void onCreate(ServiceProxy c);

	void onDestroy();

	void onStartCommand(Intent intent, int flags, int startId);

	void onEvent(Events.Dummy event);
}
