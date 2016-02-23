package org.owntracks.android.services;

import android.content.Intent;

import org.owntracks.android.support.Events;

import java.util.List;

public interface ProxyableService {
	void onCreate(ServiceProxy c);

	void onDestroy();

	int onStartCommand(Intent intent, int flags, int startId);

	void onEvent(Events.Dummy event);
}
