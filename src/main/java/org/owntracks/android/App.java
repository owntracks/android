package org.owntracks.android;


import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.owntracks.android.db.ContactLinkDao;
import org.owntracks.android.db.Dao;
import org.owntracks.android.db.DaoMaster;
import org.owntracks.android.db.MessageDao;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.model.Contact;
import org.owntracks.android.model.ContactsViewModel;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.services.ServiceBroker;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.RecyclerViewAdapter;
import org.owntracks.android.support.StatisticsProvider;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.core.CrashlyticsCore;

import de.greenrobot.event.EventBus;
import me.tatarka.bindingcollectionadapter.BindingRecyclerViewAdapter;
import me.tatarka.bindingcollectionadapter.ItemViewArg;
import me.tatarka.bindingcollectionadapter.factories.BindingRecyclerViewAdapterFactory;

public class App extends Application  {
    private static final String TAG = "App";

    private static App instance;
    private static boolean inForeground;
    private static int runningActivities = 0;
    private SimpleDateFormat dateFormater;
    private static Handler mainHanler;

    private ContactLinkDao contactLinkDao;
	private WaypointDao waypointDao;
    private MessageDao messageDao;

    private static ArrayMap<String, FusedContact> fusedContacts;
    private static ContactsViewModel contactsViewModel;

    private static HashMap<String, Contact> contacts;    /* TODO: DEPRECATED*/
    private static HashMap<String, Contact> initializingContacts;    /* TODO: DEPRECATED*/

    private static Activity currentActivity;


    public static final int MODE_ID_PRIVATE=0;
    public static final int MODE_ID_HOSTED=1;
    public static final int MODE_ID_PUBLIC=2;
    private SQLiteDatabase db;

    public static ArrayMap<String, FusedContact> getFusedContacts() {
        return fusedContacts;
    }

    @Override
	public void onCreate() {
		super.onCreate();
        instance = this;

        Fabric.with(this, new Crashlytics.Builder().core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build(), new Answers());
        Preferences.initialize(this);

        StatisticsProvider.setTime(this, StatisticsProvider.APP_START);
        Answers.getInstance().logCustom(new CustomEvent("App started").putCustomAttribute("mode", Preferences.getModeId()));

        DaoMaster.OpenHelper helper = new DaoMaster.OpenHelper(this, "org.owntracks.android.db", null) {
            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                DaoMaster.dropAllTables(db, true);
                onCreate(db);
            }
        };


		this.dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", getResources().getConfiguration().locale);
        this.mainHanler = new Handler(getMainLooper());
        this.contacts = new HashMap<String, Contact>();
        this.initializingContacts = new HashMap<String, Contact>();
        this.fusedContacts = new ArrayMap<String, FusedContact>();
        this.contactsViewModel =  new ContactsViewModel();

        //EstimoteSDK.initialize(this, getString(R.string.ESTIMOTE_API_ID), getString(R.string.ESTIMOTE_API_KEY));
        //EstimoteSDK.enableDebugLogging(true);

        ContactImageProvider.initialize(this);
        GeocodingProvider.initialize(this);

        Dao.initialize(this);
		EventBus.getDefault().register(this);
        registerActivityLifecycleCallbacks(new LifecycleCallbacks());


    }


	public static Context getContext() {
		return instance;
	}
    public static App getInstance() {
        return instance;
    }

    /*TODO: refactor to getFusedContact remove */
    public static Contact getContact(String topic) {
		return instance.contacts.get(topic);
	}

    public static FusedContact getFusedContact(String topic) {
        return fusedContacts.get(topic);
    }

    public static ContactsViewModel getContactsViewModel() {
        return contactsViewModel;
    }

    /* TODO: DEPRECATED*/
    public static HashMap<String, Contact> getCachedContacts() {
        return contacts;
    }

    public static void addFusedContact(final FusedContact c) {
        fusedContacts.put(c.getTopic(), c);

        postOnMainHandler(new Runnable() {
            @Override
            public void run() {
                contactsViewModel.items.add(c);
            }
        });
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
    public static void postOnMainHandler(Runnable r) {
        mainHanler.post(r);
    }

        /* TODO: DEPRECATED*/
    public static void removeContact(Contact c) {
        instance.contacts.remove(c.getTopic());
        EventBus.getDefault().post(new Events.ContactRemoved(c));
    }

	public static String formatDate(Date d) {
		return instance.dateFormater.format(d);
	}

	public static String getAndroidId() {
		return Secure.getString(instance.getContentResolver(), Secure.ANDROID_ID);
	}

	public static int getBatteryLevel() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = getContext().registerReceiver(null, ifilter);
		return batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : 0;
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
                //  ServiceProxy.getServiceBeacon().setBackgroundMode(false);
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
                // ServiceProxy.getServiceBeacon().setBackgroundMode(true);
            }
        });
    }

    public static boolean isInForeground() {
        return inForeground;
    }

    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    /*
     * Keeps track of running activities and if the app is in running in the foreground or background
     */
    private static final class LifecycleCallbacks implements ActivityLifecycleCallbacks {
        public void onActivityStarted(Activity activity) {
            App.runningActivities++;
            currentActivity = activity;
            if (App.runningActivities == 1) App.onEnterForeground();
        }

        public void onActivityStopped(Activity activity) {
            App.runningActivities--;
            if(currentActivity == activity)  currentActivity = null;
            if (App.runningActivities == 0) App.onEnterBackground();
        }

        public void onActivityResumed(Activity activity) {  }
        public void onActivityPaused(Activity activity) {  }
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
        public void onActivityDestroyed(Activity activity) { }
    }

}
