package st.alr.mqttitude;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import st.alr.mqttitude.model.Contact;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Preferences;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.BatteryManager;
import android.provider.Settings.Secure;
import android.widget.Toast;

import com.bugsnag.android.Bugsnag;


public class App extends Application {
    private static App instance;
    private SimpleDateFormat dateFormater;
    private HashMap<String,Contact> contacts;


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        this.dateFormater = new SimpleDateFormat("yyyy-MM-d HH:mm:ss", getResources().getConfiguration().locale);
        this.contacts = new HashMap<String,Contact>();

        Bugsnag.register(this, Preferences.getBugsnagApiKey());
        Bugsnag.setNotifyReleaseStages("production", "testing");
        

    }
    

    public static Context getContext() {
        return instance;
    }
    public static HashMap<String, Contact> getContacts() {
        return instance.contacts;
    }

    public static String formatDate(Date d) {
        return instance.dateFormater.format(d);
    }
    
    public static boolean isDebugBuild() {
        return 0 != (instance.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE);
    }
    
    public static String getAndroidId() {
        return Secure.getString(instance.getContentResolver(), Secure.ANDROID_ID);
    }

    public static int getBatteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getContext().registerReceiver(null, ifilter);
        return batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    }


    public static void showLocationNotAvailableToast() {
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.currentLocationNotAvailable), Toast.LENGTH_SHORT).show();        
    }
    
}
