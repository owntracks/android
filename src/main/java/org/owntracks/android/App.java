package org.owntracks.android;


import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.owntracks.android.db.ContactLinkDao;
import org.owntracks.android.db.DaoMaster;
import org.owntracks.android.db.DaoSession;
import org.owntracks.android.db.MessageDao;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.model.Contact;
import org.owntracks.android.services.ServiceBroker;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Statistics;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.database.sqlite.SQLiteDatabase;
import android.os.BatteryManager;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.maps.MapsInitializer;

import de.greenrobot.event.EventBus;

public class App extends Application {
    private static final String TAG = "App";

    private static App instance;
    private static boolean inForeground;
    private SimpleDateFormat dateFormater;

    private ContactLinkDao contactLinkDao;
	private WaypointDao waypointDao;
    private MessageDao messageDao;

    private static HashMap<String, Contact> contacts;
    private static HashMap<String, Contact> initializingContacts;

    public static final int MODE_ID_PRIVATE=0;
    public static final int MODE_ID_HOSTED=1;
    public static final int MODE_ID_PUBLIC=2;
    private SQLiteDatabase db;

    @Override
	public void onCreate() {
		super.onCreate();
        instance = this;

        if(BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        } else {
            Fabric.with(this, new Crashlytics(), new Answers());
        }

        Preferences preferences = new Preferences(this);
        Statistics.setTime(this, Statistics.APP_START);

        Answers.getInstance().logCustom(new CustomEvent("App started").putCustomAttribute("mode", Preferences.getModeId()));

        DaoMaster.OpenHelper helper = new DaoMaster.OpenHelper(this, "org.owntracks.android.db", null) {
            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                DaoMaster.dropAllTables(db, true);
                onCreate(db);
            }
        };

        db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();
		this.contactLinkDao = daoSession.getContactLinkDao();
		this.waypointDao = daoSession.getWaypointDao();
        this.messageDao = daoSession.getMessageDao();

		this.dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", getResources().getConfiguration().locale);
		this.contacts = new HashMap<String, Contact>();
        this.initializingContacts = new HashMap<String, Contact>();

		//Initialize Google Maps and BitmapDescriptorFactory
		MapsInitializer.initialize(getApplicationContext());
		EventBus.getDefault().register(this);

    }

	public static MessageDao getMessageDao() {
		return instance.messageDao;
	}

    public static SQLiteDatabase getDb() { return instance.db; }
    public static WaypointDao getWaypointDao() {
        return instance.waypointDao;
    }

	public static ContactLinkDao getContactLinkDao() {
		return instance.contactLinkDao;
	}

	public static Context getContext() {
		return instance;
	}

	public static Contact getContact(String topic) {
		return instance.contacts.get(topic);
	}


    public static HashMap<String, Contact> getCachedContacts() {
        return contacts;
    }

    public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
        if(e.getState() == ServiceBroker.State.CONNECTING) {
            //Log.v(TAG, "State changed to connecting. Clearing cached contacts");
            instance.contacts.clear();
        }
    }



    public void onEvent(Events.ModeChanged e) {
        instance.contacts.clear();
    }

    public static void addContact(Contact c) {
        instance.contacts.put(c.getTopic(), c);
        initializingContacts.remove(c.getTopic());

        EventBus.getDefault().post(new Events.ContactAdded(c));
    }

    public static void addUninitializedContact(Contact c) {
        instance.initializingContacts.put(c.getTopic(), c);
    }

    public static Contact getInitializingContact(String topic) {
        return instance.initializingContacts.get(topic);
    }

    public static void removeContact(Contact c) {
        instance.contacts.remove(c.getTopic());
        EventBus.getDefault().post(new Events.ContactRemoved(c));
    }

	public static String formatDate(Date d) {
		return instance.dateFormater.format(d);
	}

	public static boolean isDebugBuild() {
		return 0 != (instance.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE);
	}

	public static String getAndroidId() {
		return Secure.getString(instance.getContentResolver(),
				Secure.ANDROID_ID);
	}

	public static int getBatteryLevel() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = getContext().registerReceiver(null, ifilter);
		return batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : 0;
	}

	public static void showLocationNotAvailableToast() {
		Toast.makeText(App.getContext(), App.getContext()
						.getString(R.string.currentLocationNotAvailable), Toast.LENGTH_SHORT).show();
	}


	public void onEventMainThread(Events.BrokerChanged e) {
		contacts.clear();
	}

    public static void onEnterForeground() {
        Log.v(TAG, "onEnterForeground");
        inForeground = true; 
        ServiceProxy.runOrBind(getContext(), new Runnable() {

            @Override
            public void run() {
                ServiceProxy.getServiceLocator().enableForegroundMode();
                ServiceProxy.getServiceBeacon().setBackgroundMode(false);
            }
        });
    }

    public static void onEnterBackground() {
        Log.v(TAG, "onEnterBackground");
        inForeground = false;
        ServiceProxy.runOrBind(getContext(), new Runnable() {

            @Override
            public void run() {
                ServiceProxy.getServiceLocator().enableBackgroundMode();
                ServiceProxy.getServiceBeacon().setBackgroundMode(true);
            }
        });
    }

    public static boolean isInForeground() {
        return inForeground;
    }
}
