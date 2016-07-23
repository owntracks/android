package org.owntracks.android.services;

import java.io.Closeable;
import java.util.HashMap;
import java.util.LinkedList;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.owntracks.android.support.StatisticsProvider;
import org.owntracks.android.support.interfaces.ServiceMessageEndpoint;
import org.owntracks.android.support.receiver.ReceiverProxy;

import de.greenrobot.event.EventBus;

public class ServiceProxy extends ServiceBindable {
	private static final String TAG = "ServiceProxy";

    public static final String WAKELOCK_TAG_BROKER_PING = "org.owntracks.android.wakelock.broker.ping";
    public static final String WAKELOCK_TAG_BROKER_NETWORK = "org.owntracks.android.wakelock.broker.network";
    public static final String WAKELOCK_TAG_BROKER_CONNECTIONLOST = "org.owntracks.android.wakelock.broker.connectionlost";


    public static final String SERVICE_APP = "A";
	public static final String SERVICE_LOCATOR = "L";
	public static final String SERVICE_MESSAGE_MQTT = "SM";
	public static final String SERVICE_MESSAGE_HTTP = "SH";
	public static final String SERVICE_NOTIFICATION = "N";
	public static final String SERVICE_BEACON = "BE";
	public static final String SERVICE_MESSAGE = "M";


	public static final String KEY_SERVICE_ID = "srvID";
	private static ServiceProxy instance;
	private static final HashMap<String, ProxyableService> services = new HashMap<>();
	private static final LinkedList<Runnable> runQueue = new LinkedList<>();
	private static ServiceProxyConnection connection;
	private static boolean bound = false;
    private static boolean attemptingToBind = false;




	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	protected void onStartOnce() {
		instance = this;

		StatisticsProvider.setTime(StatisticsProvider.SERVICE_PROXY_START);

		instantiateService(SERVICE_APP);
		instantiateService(SERVICE_NOTIFICATION);

		instantiateService(SERVICE_MESSAGE);

        instantiateService(SERVICE_LOCATOR);
        instantiateService(SERVICE_BEACON);
	}

	public static ServiceProxy getInstance() {
		return instance;
	}

	@Override
	public void onDestroy() {
		for (ProxyableService p : services.values()) {
			EventBus.getDefault().unregister(p);
			p.onDestroy();
		}

		super.onDestroy();

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int r = super.onStartCommand(intent, flags, startId);
		ProxyableService s = getServiceForIntent(intent);
		Log.v(TAG, "onStartCommand getServiceForIntent:"+s);
		if (s != null)
			s.onStartCommand(intent, flags, startId);
		return r;
	}

	public static ProxyableService getService(String id) {
		return services.get(id);
	}

	public static ProxyableService instantiateService(String id) {
		if (services.containsKey(id))
			return services.get(id);

		ProxyableService p = null;
        switch (id) {
            case SERVICE_APP:
                p = new ServiceApplication();
                break;
            case SERVICE_MESSAGE_MQTT:
                p = new ServiceMessageMqttExperimental();
                break;
			case SERVICE_MESSAGE_HTTP:
				p = new ServiceMessageHttp();
				break;
			case SERVICE_LOCATOR:
                p = new ServiceLocator();
                break;
            case SERVICE_BEACON:
                p = new ServiceBeacon();
                break;
			case SERVICE_NOTIFICATION:
				p = new ServiceNotification();
				break;
			case SERVICE_MESSAGE:
				p = new ServiceMessage();
				break;
		}

		if(p == null)
			return null;

		services.put(id, p);
		p.onCreate(instance);
		EventBus.getDefault().registerSticky(p);
		return p;
	}

	public static ServiceApplication getServiceApplication() {
		return (ServiceApplication) getService(SERVICE_APP);
	}

	public static ServiceLocator getServiceLocator() {
		return (ServiceLocator) getService(SERVICE_LOCATOR);
	}

	public static ServiceMessageMqtt getServiceMessageMqtt() {
		return (ServiceMessageMqtt) getService(SERVICE_MESSAGE_MQTT);
	}

	public static ServiceMessageHttp getServiceMessageHttp() {
		return (ServiceMessageHttp) getService(SERVICE_MESSAGE_HTTP);
	}
    public static ServiceBeacon getServiceBeacon() {
        return (ServiceBeacon) getService(SERVICE_BEACON);
    }
	public static ServiceNotification getServiceNotification() {
		return (ServiceNotification) getService(SERVICE_NOTIFICATION);
	}
	public static ServiceMessage getServiceMessage() {
		return (ServiceMessage) getService(SERVICE_MESSAGE);
	}


	public static ProxyableService getServiceForIntent(Intent i) {
		if ((i != null) && (i.getStringExtra(KEY_SERVICE_ID) != null))
			return getService(i.getStringExtra(KEY_SERVICE_ID));
		else
			return null;

	}

	public static PendingIntent getBroadcastIntentForService(Context c,  String targetServiceId, String action, Bundle extras) {
		return getBroadcastIntentForService(c, targetServiceId, action, extras, -1);
	}

	public static PendingIntent getBroadcastIntentForService(Context c,  String targetServiceId, String action, Bundle extras, int flags) {
		Intent i = new Intent().setClass(c, ReceiverProxy.class);
		i.setAction(action);

		if (extras != null)
			i.putExtras(extras);
		i.putExtra(KEY_SERVICE_ID, targetServiceId);

		return PendingIntent.getBroadcast(c, 0, i, flags != -1 ? flags : PendingIntent.FLAG_CANCEL_CURRENT);
	}




	public static PendingIntent getPendingIntentForService(Context c,
			String targetServiceId, String action, Bundle extras) {
		return getPendingIntentForService(c, targetServiceId, action, extras,
				PendingIntent.FLAG_CANCEL_CURRENT);
	}

	public static PendingIntent getPendingIntentForService(Context c,
			String targetServiceId, String action, Bundle extras, int flags) {
		Intent i = new Intent().setClass(c, ServiceProxy.class);
		i.setAction(action);

		if (extras != null)
			i.putExtras(extras);
		i.putExtra(KEY_SERVICE_ID, targetServiceId);

		return PendingIntent.getService(c, 0, i, flags);

	}

	public static void stopService(ProxyableService service) {
		//TODO
	}

	public final static class ServiceProxyConnection implements Closeable {
		private final Context context;
		private final ServiceConnection serviceConnection;

		private ServiceProxyConnection(Context context,
				ServiceConnection serviceConnection) {
			this.context = context;
			this.serviceConnection = serviceConnection;
		}

		@Override
		public void close() {
            attemptingToBind = false;

            if (bound) {
				this.context.unbindService(this.serviceConnection);
				bound = false;
			}
		}

		public ServiceConnection getServiceConnection() {
			return this.serviceConnection;
		}

	}

    // No bind, only acting on static methods and tearing down service connection anyway
	public static void closeServiceConnection() {
		if ((getServiceConnection() != null) && bound)
            getServiceConnection().close();
	}

	public static ServiceProxyConnection getServiceConnection() {
		return connection;
	}

	public static void runOrBind(Context context, Runnable runnable) {
		if ((instance != null) && (getServiceConnection() != null)) {

            runnable.run();
			return;
		}

		if (getServiceConnection() == null) {
			ServiceConnection c = new ServiceConnection() {
				@Override
				public void onServiceDisconnected(ComponentName name) {
					bound = false;
				}

				@Override
				public void onServiceConnected(ComponentName name, IBinder binder) {

                    bound = true;
                    attemptingToBind = false;

					for (Runnable r : runQueue)
						r.run();
					runQueue.clear();

				}
			};
			connection = new ServiceProxyConnection(context, c);
		}

		runQueue.addLast(runnable);

        try {
            if (!attemptingToBind) { // Prevent accidental bind during close
                attemptingToBind = true;
                context.bindService(new Intent(context, ServiceProxy.class), connection.getServiceConnection(), Context.BIND_AUTO_CREATE);
            }
        } catch (Exception e) {
            Log.e("ServiceProxy", "bind exception ");
            e.printStackTrace();
            attemptingToBind = false;
        }
	}
}
