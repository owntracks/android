package org.owntracks.android;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.owntracks.android.activities.ActivityMap;
import org.owntracks.android.db.Dao;
import org.owntracks.android.injection.components.AppComponent;
import org.owntracks.android.injection.components.DaggerAppComponent;
import org.owntracks.android.injection.modules.AppModule;
import org.owntracks.android.model.ContactsViewModel;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.EncryptionProvider;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.StatisticsProvider;

import android.app.Activity;
import android.app.Application;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.text.format.DateUtils;
import android.util.Log;

import de.greenrobot.event.EventBus;
import io.realm.Realm;
import timber.log.Timber;

public class App extends Application  {
    private static final String TAG = "App";

    private static App sInstance;
    private static SimpleDateFormat dateFormater;
    private static SimpleDateFormat dateFormaterToday;

    private static Handler mainHandler;
    private static Handler backgroundHandler;

    private static HashMap<String, FusedContact> fusedContacts;
    private static ContactsViewModel contactsViewModel;
    private static Activity currentActivity;
    private static boolean inForeground;
    private static int runningActivities = 0;

    public static final int MODE_ID_MQTT_PRIVATE =0;
    public static final int MODE_ID_MQTT_PUBLIC =2;
    public static final int MODE_ID_HTTP_PRIVATE = 3;

    private static AppComponent sAppComponent = null;


    public static HashMap<String, FusedContact> getFusedContacts() {
        return fusedContacts;
    }

    @Override
	public void onCreate() {
		super.onCreate();
        Log.d(TAG, "trace / App onCreate start" + System.currentTimeMillis());
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree() {
                @Override
                protected String createStackElementTag(StackTraceElement element) {
                    return super.createStackElementTag(element) + "/" + element.getMethodName() + "/" + element.getLineNumber();

                }
            });
        }

        sInstance = this;
        sInstance = this;
        sAppComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();


        dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", getResources().getConfiguration().locale);
        dateFormaterToday = new SimpleDateFormat("HH:mm:ss", getResources().getConfiguration().locale);

        HandlerThread mServiceHandlerThread = new HandlerThread("ServiceThread");
        mServiceHandlerThread.start();

        backgroundHandler = new Handler(mServiceHandlerThread.getLooper());
        mainHandler = new Handler(getMainLooper());
        fusedContacts = new HashMap<>();
        contactsViewModel =  new ContactsViewModel();

        checkFirstStart();


        StatisticsProvider.initialize(App.getInstance());
        Preferences.initialize(App.getInstance());
        Parser.initialize(App.getInstance());
        ContactImageProvider.initialize(App.getInstance());
        GeocodingProvider.initialize(App.getInstance());
        Dao.initialize(App.getInstance());
        EncryptionProvider.initialize();
        Log.d(TAG, "trace / App async init end" + System.currentTimeMillis());

        ServiceProxy.runOrBind(sInstance, new Runnable() {
            @Override
            public void run() {
                Timber.v("trace loading services %s", System.currentTimeMillis());
            }
        });

        //EventBus.getDefault().register(this);
        Log.d(TAG, "trace / App onCreate done" + System.currentTimeMillis());

    }

    public static App getInstance() { return sInstance; }

    public static AppComponent getAppComponent() { return sAppComponent; }

    public static Realm getRealm() { return sAppComponent.realm(); }

    public static Resources getRes() { return sInstance.getResources(); }


    public static void enableForegroundBackgroundDetection() {
        sInstance.registerActivityLifecycleCallbacks(new LifecycleCallbacks());
        sInstance.registerScreenOnReceiver();
    }



    public static Context getContext() {
		return sInstance;
	}

    public static FusedContact getFusedContact(String topic) {
        return fusedContacts.get(topic);
    }

    public static ContactsViewModel getContactsViewModel() {
        return contactsViewModel;
    }


    public static void addFusedContact(final FusedContact c) {
        fusedContacts.put(c.getId(), c);

        postOnMainHandler(new Runnable() {
            @Override
            public void run() {
                contactsViewModel.items.add(c);
            }
        });
        EventBus.getDefault().post(c);
    }

    public static void updateFusedContact(FusedContact c) {
        EventBus.getDefault().post(c);
    }


    public static void clearFusedContacts() {
        fusedContacts.clear();
        postOnMainHandler(new Runnable() {
            @Override
            public void run() {
                contactsViewModel.items.clear();
            }
        });
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.ModeChanged e) {
        clearFusedContacts();
        ContactImageProvider.invalidateCache();
    }

    public static void postOnMainHandlerDelayed(Runnable r, long delayMilis) {
        mainHandler.postDelayed(r, delayMilis);
    }

    public static void postOnMainHandler(Runnable r) {
        mainHandler.post(r);
    }

    public static void postOnBackgroundHandlerDelayed(Runnable r, long delayMilis) {
        backgroundHandler.postDelayed(r, delayMilis);
    }

    public static void postOnBackgroundHandler(Runnable r) {
        backgroundHandler.post(r);
    }


    public static String formatDate(long tstSeconds) {
        return formatDate(new Date(TimeUnit.SECONDS.toMillis(tstSeconds)));
    }

	public static String formatDate(Date d) {
        if(DateUtils.isToday(d.getTime())) {
            return dateFormaterToday.format(d);
        } else {
            return dateFormater.format(d);
        }
	}

	public static String getAndroidId() {
		return Secure.getString(sInstance.getContentResolver(), Secure.ANDROID_ID);
	}

	public static int getBatteryLevel() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = getContext().registerReceiver(null, ifilter);
		return batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : 0;
	}

    @SuppressWarnings("unused")
    public void onEvent(Events.BrokerChanged e) {
        clearFusedContacts();
    }

    private static void onEnterForeground() {
        inForeground = true;
        ServiceProxy.runOrBind(getContext(), new Runnable() {

            @Override
            public void run() {
                ServiceProxy.getServiceLocator().enableForegroundMode();
                ServiceProxy.getServiceBeacon().enableForegroundMode();
            }
        });
    }

    private static void onEnterBackground() {
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

    public static void removeContact(FusedContact contact) {
        //TODO
    }

    public static Class<?> getRootActivityClass(){
        return ActivityMap.class;
    }


    /*
     * Keeps track of running activities and if the app is in running in the foreground or background
     */
    private static final class LifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
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

        // System Defined Broadcast
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


    // Checks if the app is started for the first time.
    // On every new install this returns true for the first time and false afterwards
    private void checkFirstStart() {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);

        if(p.getBoolean(Preferences.Keys._FIST_START, true)) {
            Log.v(TAG, "Initial application launch");
            String uuid = UUID.randomUUID().toString().toUpperCase();

            p.edit().putBoolean(Preferences.Keys._FIST_START , false).putBoolean(Preferences.Keys._SETUP_NOT_COMPLETED , true).putString(Preferences.Keys._DEVICE_UUID, "A"+uuid.substring(1)).apply();

        } else {
            Log.v(TAG, "Consecutive application launch");
        }
    }

}
