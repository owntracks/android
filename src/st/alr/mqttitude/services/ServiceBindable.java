package st.alr.mqttitude.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.lang.ref.WeakReference;

public abstract class ServiceBindable extends Service {
    protected boolean started;
    protected ServiceBinder binder;
    private final String TAG  = "ServiceBindable";
    
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.v(this.TAG, "onCreate");
        binder = new ServiceBinder(this);
    }
    
    abstract protected void onStartOnce();

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(this.TAG, "onBind");
        if(!started) {
            started = true;
            onStartOnce();
        }
        return binder;
    }
    
    
    public class ServiceBinder extends Binder
    {
        private WeakReference<ServiceBindable> mService;

        public ServiceBinder(ServiceBindable serviceBindable) {
            mService = new WeakReference<ServiceBindable>(serviceBindable);
        }

        public ServiceBindable getService() {
            return mService.get();
        }
        public void close() {
            mService = null;
        }
    }
    @Override
    public void onDestroy()
    {

        if (binder != null) {
            binder.close();
            binder = null;
        }
        super.onDestroy();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(this.TAG, "onStartCommand");
        if(!started) {
            started = true;
            onStartOnce();
        }
        
        return Service.START_STICKY;
    }

}
