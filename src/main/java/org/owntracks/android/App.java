package org.owntracks.android;


import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.owntracks.android.db.ContactLinkDao;
import org.owntracks.android.db.DaoMaster;
import org.owntracks.android.db.DaoMaster.OpenHelper;
import org.owntracks.android.db.DaoSession;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.model.Contact;
import org.owntracks.android.services.ServiceBroker;
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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapsInitializer;

import de.greenrobot.event.EventBus;

public class App extends Application {
	private static App instance;
	private SimpleDateFormat dateFormater;

    private ContactLinkDao contactLinkDao;
	private WaypointDao waypointDao;
    private static HashMap<String, Contact> contacts;
    private static HashMap<String, Contact> initializingContacts;

    public static final int MODE_ID_PRIVATE=0;
    public static final int MODE_ID_HOSTED=1;
    public static final int MODE_ID_PUBLIC=2;


	@Override
	public void onCreate() {
		super.onCreate();
        instance = this;
        Preferences preferences = new Preferences(this);

        if(!BuildConfig.DEBUG) {
            Log.v(this.toString(), "Fabric.io crash reporting enabled");
            Fabric.with(this, new Crashlytics());
            //final Fabric fabric = new Fabric.Builder(this).kits(new Crashlytics()).build();
        }


        OpenHelper helper = new OpenHelper(this, "org.owntracks.android.db", null) {
            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                Log.v(this.toString(), "Migrating db from " + oldVersion  + " to  " + newVersion);
                if(oldVersion == 1 && newVersion == 2) {
                    DaoMaster.dropAllTables(db, true);
                    DaoMaster.createAllTables(db, true);
                } else if (oldVersion == 2 && newVersion == 3) {
                    DaoMaster.dropAllTables(db, true);
                    DaoMaster.createAllTables(db, true);
                }
            }
        };
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();
		this.contactLinkDao = daoSession.getContactLinkDao();
		this.waypointDao = daoSession.getWaypointDao();

		this.dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", getResources().getConfiguration().locale);
		this.contacts = new HashMap<String, Contact>();
        this.initializingContacts = new HashMap<String, Contact>();

		//Initialize Google Maps and BitmapDescriptorFactory
		MapsInitializer.initialize(getApplicationContext());
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
        if(e.getState() == ServiceBroker.State.CONNECTING) {
            //Log.v(this.toString(), "State changed to connecting. Clearing cached contacts");
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

    public static void changeProfilePublic(){
        //TODO:
        // Set profile
        // send event
    }

    public static void changeProfilePrivate(){
        //TODO:
        // Set profile
        // send event
    }
}
