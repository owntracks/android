package org.owntracks.android;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.owntracks.android.db.ContactLinkDao;
import org.owntracks.android.db.DaoMaster;
import org.owntracks.android.db.DaoMaster.OpenHelper;
import org.owntracks.android.db.DaoSession;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.model.Contact;
import org.owntracks.android.support.Defaults;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
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

import com.bugsnag.android.Bugsnag;

import de.greenrobot.event.EventBus;

public class App extends Application {
	private static App instance;
	private SimpleDateFormat dateFormater;

    private ContactLinkDao contactLinkDao;
	private WaypointDao waypointDao;
    private static HashMap<String, Contact> contacts;

	@Override
	public void onCreate() {
		super.onCreate();
        instance = this;

        Bugsnag.init(this, Preferences.getBugsnagApiKey());
        Bugsnag.setNotifyReleaseStages("production", "testing");

        Preferences.handleFirstStart();
        OpenHelper helper = new OpenHelper(this, "org.owntracks.android.db", null) {
            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                Log.v(this.toString(), "Migrating db from " + oldVersion  + " to  " + newVersion);
                // Add migrations here
            }
        };
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();
		this.contactLinkDao = daoSession.getContactLinkDao();
		this.waypointDao = daoSession.getWaypointDao();

		this.dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", getResources().getConfiguration().locale);
		this.contacts = new HashMap<String, Contact>();

		EventBus.getDefault().register(this);

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

	public static Contact getContact(String topic) {
		return instance.contacts.get(topic);
	}


    public static HashMap<String, Contact> getCachedContacts() {
        return contacts;
    }

    public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
        if(e.getState() == Defaults.State.ServiceBroker.CONNECTING) {
            //Log.v(this.toString(), "State changed to connecting. Clearing cached contacts");
            instance.contacts.clear();
        }
    }

    public static void addContact(Contact c) {
        instance.contacts.put(c.getTopic(), c);
        EventBus.getDefault().post(new Events.ContactAdded(c));
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


}
