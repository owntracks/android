package st.alr.mqttitude;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


import st.alr.mqttitude.model.Contact;
import st.alr.mqttitude.support.Defaults;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.BatteryManager;
import android.provider.Settings.Secure;

import com.bugsnag.android.Bugsnag;


public class App extends Application {
    private static App instance;
    private SimpleDateFormat dateFormater;
    private Map<String,Contact> contacts;


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        this.dateFormater = new SimpleDateFormat("yy/MM/d HH:mm:ss", getResources().getConfiguration().locale);
        this.contacts = new HashMap<String,Contact>();

        Bugsnag.register(this, Defaults.BUGSNAG_API_KEY);
        Bugsnag.setNotifyReleaseStages("production", "testing");
        

    }
    

    public static Context getContext() {
        return instance;
    }
    public static Map<String, Contact> getContacts() {
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
    
}
