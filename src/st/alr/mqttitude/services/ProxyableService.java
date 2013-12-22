package st.alr.mqttitude.services;

import android.content.Context;
import android.content.Intent;

public interface ProxyableService {
    public void onCreate(ServiceProxy c);
    public void onDestroy();    
    public int onStartCommand(Intent intent, int flags, int startId);
}
