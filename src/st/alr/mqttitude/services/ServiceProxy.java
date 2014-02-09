
package st.alr.mqttitude.services;

import java.util.HashMap;
import java.util.LinkedList;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.util.Log;
import de.greenrobot.event.EventBus;

public class ServiceProxy extends ServiceBindable {
    public static final String SERVICE_APP = "1:App";
    public static final String SERVICE_LOCATOR = "2:Loc";
    public static final String SERVICE_BROKER = "3:Brk";
    public static final String KEY_SERVICE_ID = "srvID";
    private static ServiceProxy instance;
    private static LinkedList<Runnable> runQueue = new LinkedList<Runnable>();
    private static HashMap<String, ProxyableService> services = new HashMap<String, ProxyableService>();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onStartOnce() {
        instantiateService(SERVICE_APP);
        instantiateService(SERVICE_BROKER);
        instantiateService(SERVICE_LOCATOR);
        instance = this;
    }

    
    public static ServiceProxy getInstance() {
        return instance;
    }

    public static boolean runOrQueueRunnable(Runnable r, Activity a, ServiceConnection c) {
        
        if(instance != null) {
            r.run();
            return true;
        } else {
            runQueue.addLast(r);
            a.bindService(new Intent(a, ServiceProxy.class), c, Context.BIND_AUTO_CREATE);
            return false;
        }
    }
    
    public static void runQueuedRunnables(){
        Log.v("ServiceProxy", "Running " + runQueue.size() + " runnables");
        for(Runnable r : runQueue) 
            r.run();
        runQueue.clear();
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
        int r = super.onStartCommand(intent, flags, startId); // Invokes onStartOnce(...) the fist time to initialize the service

        ProxyableService s = getServiceForIntent(intent);
        if (s != null)
            s.onStartCommand(intent, flags, startId);

        return r;
    }

    public static ProxyableService getService(String id) {
        return services.get(id);
    }

    private ProxyableService instantiateService(String id) {
        ProxyableService p = services.get(id);
        if (p != null)
            return p;

        if (id.equals(SERVICE_APP))
            p = new ServiceApplication();
        else if (id.equals(SERVICE_BROKER))
            p = new ServiceBroker();
        else if (id.equals(SERVICE_LOCATOR))
            p = new ServiceLocator();

        services.put(id, p);
        p.onCreate(this);
        EventBus.getDefault().registerSticky(p);

        return p;
    }

    public static ServiceApplication getServiceApplication() {
        return (ServiceApplication) getService(SERVICE_APP);
    }

    public static ServiceLocator getServiceLocator() {
        return (ServiceLocator) getService(SERVICE_LOCATOR);
    }

    public static ServiceBroker getServiceBroker() {
        return (ServiceBroker) getService(SERVICE_BROKER);
    }

    public static ProxyableService getServiceForIntent(Intent i) {
        if ((i != null) && (i.getStringExtra(KEY_SERVICE_ID) != null))
            return getService(i.getStringExtra(KEY_SERVICE_ID));
        else
            return null;
        
    }

    public static PendingIntent getPendingIntentForService(Context c, String targetServiceId, String action, Bundle extras) {
        return getPendingIntentForService(c, targetServiceId, action, extras, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent getPendingIntentForService(Context c, String targetServiceId, String action, Bundle extras, int flags) {
        Intent i = new Intent().setClass(c, ServiceProxy.class);
        i.setAction(action);

        if (extras != null)
            i.putExtras(extras);
        i.putExtra(KEY_SERVICE_ID, targetServiceId);

        return PendingIntent.getService(c, 0, i, flags);

    }
}
