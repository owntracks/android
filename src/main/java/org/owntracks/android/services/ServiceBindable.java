package org.owntracks.android.services;

import java.lang.ref.WeakReference;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public abstract class ServiceBindable extends Service {
	private static final String TAG = "ServiceBindable";

	protected boolean started;
	protected ServiceBinder binder;


    @Override
	public void onCreate() {
		super.onCreate();
		this.binder = new ServiceBinder(this);
	}

	abstract protected void 	onStartOnce();

	@Override
	public IBinder onBind(Intent intent) {
		if (!this.started) {
			this.started = true;
			onStartOnce();
		}
		return this.binder;
	}

	public class ServiceBinder extends Binder {
		private WeakReference<ServiceBindable> mService;

		public ServiceBinder(ServiceBindable serviceBindable) {
			this.mService = new WeakReference<ServiceBindable>(serviceBindable);
		}

		public ServiceBindable getService() {
			return this.mService.get();
		}

		public void close() {
			this.mService = null;
		}
	}

	@Override
	public void onDestroy() {

		if (this.binder != null) {
			this.binder.close();
			this.binder = null;
		}
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!this.started) {
			this.started = true;
			// Called when the service is started for the first time. Shields
			// from multiple calls of startService(...) to invoke the code
			// multiple times
			onStartOnce();
		}

		return Service.START_STICKY;
	}

}
