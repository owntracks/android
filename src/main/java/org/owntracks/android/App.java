package org.owntracks.android;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.owntracks.android.db.Dao;
import org.owntracks.android.model.Contact;
import org.owntracks.android.model.ContactsViewModel;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.services.ServiceBroker;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.EncryptionProvider;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.StatisticsProvider;

import android.app.Activity;
import android.app.Application;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import de.greenrobot.event.EventBus;

public class App extends Application  {
    private static final String TAG = "App";

    private static App instance;
    private static boolean inForeground;
    private static int runningActivities = 0;
    private SimpleDateFormat dateFormater;
    private static Handler mainHanler;

    private static ArrayMap<String, FusedContact> fusedContacts;
    private static ContactsViewModel contactsViewModel;

    private static HashMap<String, Contact> contacts;    /* TODO: DEPRECATED*/
    private static Activity currentActivity;


    public static final int MODE_ID_PRIVATE=0;
    public static final int MODE_ID_HOSTED=1;
    public static final int MODE_ID_PUBLIC=2;

    public static ArrayMap<String, FusedContact> getFusedContacts() {
        return fusedContacts;
    }

    @Override
	public void onCreate() {
		super.onCreate();
        instance = this;

        Preferences.initialize(this);

        StatisticsProvider.setTime(this, StatisticsProvider.APP_START);

		this.dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", getResources().getConfiguration().locale);
        mainHanler = new Handler(getMainLooper());
        contacts = new HashMap<>();
        fusedContacts = new ArrayMap<>();
        contactsViewModel =  new ContactsViewModel();


        ContactImageProvider.initialize(this);
        GeocodingProvider.initialize(this);
        Dao.initialize(this);
        EncryptionProvider.initialize();
		EventBus.getDefault().register(this);
        registerActivityLifecycleCallbacks(new LifecycleCallbacks());
        registerScreenOnReceiver();

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
                ServiceProxy.getServiceBeacon().enableForegroundMode();
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
                ServiceProxy.getServiceBeacon().enableBackgroundMode();

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
    private void registerScreenOnReceiver() {
        final IntentFilter theFilter = new IntentFilter();
        /** System Defined Broadcast */
        theFilter.addAction(Intent.ACTION_SCREEN_ON);
        theFilter.addAction(Intent.ACTION_SCREEN_OFF);

        // Sets foreground and background modes based on device lock and unlock if the app is active




        BroadcastReceiver screenOnOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String strAction = intent.getAction();

                KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

                if (strAction.equals(Intent.ACTION_SCREEN_OFF) || strAction.equals(Intent.ACTION_SCREEN_ON))
                {
                    if( myKM.inKeyguardRestrictedInputMode())
                    {
                        if(App.isInForeground())
                            App.onEnterBackground();
                    } else
                    {
                        if(App.isInForeground())
                            App.onEnterBackground();

                    }
                }
            }
        };

        getApplicationContext().registerReceiver(screenOnOffReceiver, theFilter);

    }


}
