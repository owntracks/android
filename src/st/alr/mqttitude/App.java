package st.alr.mqttitude;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import st.alr.mqttitude.db.ContactLinkDao;
import st.alr.mqttitude.db.DaoMaster;
import st.alr.mqttitude.db.DaoSession;
import st.alr.mqttitude.db.WaypointDao;
import st.alr.mqttitude.db.DaoMaster.DevOpenHelper;
import st.alr.mqttitude.model.Contact;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Preferences;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.database.sqlite.SQLiteDatabase;
import android.os.BatteryManager;
import android.provider.Settings.Secure;
import android.widget.Toast;

import com.bugsnag.android.Bugsnag;


public class App extends Application {
    private static App instance;
    private SimpleDateFormat dateFormater;
    private HashMap<String,Contact> contacts;

    private SQLiteDatabase db;
    private DevOpenHelper helper;
    private DaoSession daoSession;
    private DaoMaster daoMaster;
    private ContactLinkDao contactLinkDao;
    private WaypointDao waypointDao;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        this.helper = new DaoMaster.DevOpenHelper(this, "mqttitude-db", null);
        this.db = this.helper.getWritableDatabase();
        this.daoMaster = new DaoMaster(this.db);
        this.daoSession = this.daoMaster.newSession();
        this.contactLinkDao = this.daoSession.getContactLinkDao();
        this.waypointDao = this.daoSession.getWaypointDao();
        
        this.dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", getResources().getConfiguration().locale);
        this.contacts = new HashMap<String,Contact>();

        Bugsnag.register(this, Preferences.getBugsnagApiKey());
        Bugsnag.setNotifyReleaseStages("production", "testing");
        

    }
    
    public static WaypointDao getWaypointDao() {
        return instance.waypointDao;
    }

    public static ContactLinkDao getContactLinkDao() {
        return instance.contactLinkDao;
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
