package st.alr.mqttitude.services;

import java.util.HashMap;
import android.content.Intent;
import android.util.Log;

import de.greenrobot.event.EventBus;

public class ServiceProxy extends ServiceBindable {
    public static final Integer SERVICE_APP = 1;
    public static final Integer SERVICE_LOCATOR = 2;
    public static final Integer SERVICE_BROKER = 3;
    private static ServiceProxy instance;
    private static HashMap<Integer, ProxyableService> services = new HashMap<Integer, ProxyableService>();
    
    @Override
    public void onCreate(){
        super.onCreate();
    }
    
    @Override
    protected void onStartOnce() {        
        Log.v(this.toString(), "ServiceApplication starting");
        instantiateService(SERVICE_APP);
        instantiateService(SERVICE_BROKER);
        instantiateService(SERVICE_LOCATOR);
        instance = this;
    }
    
    
    
    public static ServiceProxy getInstance() {
        return instance;
    }

    @Override
    public void onDestroy(){
        for (ProxyableService p : services.values()) {
            EventBus.getDefault().unregister(p);
            p.onDestroy();
        }
        
        super.onDestroy();

    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Invokes onStartOnce(...) the fist time to initialize the service         
        int r = super.onStartCommand(intent, flags, startId); 
        
        Log.e(this.toString(), "onStartCommand");

        
        if(intent != null)
            Log.e(this.toString(), "onStartCommand with intent" + intent.toString());
            for (ProxyableService p : services.values())
                p.onStartCommand(intent, flags, startId);
        
        return r;        
    }
        
    public static ProxyableService getService(Integer id){
        return services.get(id);
    }
    
    private ProxyableService instantiateService(Integer id){
        ProxyableService p = services.get(id);
        if(p != null)
            return p;
        
                
        if(id == SERVICE_APP)
            p = new ServiceApplication();
        else if(id == SERVICE_BROKER)
            p = new ServiceBroker();
        else if(id == SERVICE_LOCATOR)
            p = new ServiceLocatorFused();
             

        services.put(id, p);
        p.onCreate(this);
        EventBus.getDefault().registerSticky(p);

        return p;
    }

    public static ServiceApplication getServiceApplication() {
        return (ServiceApplication) getService(SERVICE_APP);
    }
    public static ServiceLocatorFused getServiceLocator() {
        return (ServiceLocatorFused) getService(SERVICE_LOCATOR);
    }
    public static ServiceBroker getServiceBroker() {
        return (ServiceBroker) getService(SERVICE_BROKER);
    }
}
