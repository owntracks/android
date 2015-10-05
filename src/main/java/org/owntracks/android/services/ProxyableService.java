package org.owntracks.android.services;

import android.content.Intent;

import org.owntracks.android.support.Events;

public interface ProxyableService {
	public void onCreate(ServiceProxy c);

	public void onDestroy();

	public int onStartCommand(Intent intent, int flags, int startId);

	public void onEvent(Events.Dummy event);

}
