package org.owntracks.android;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.activities.ActivityWelcome;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.db.Dao;
import org.owntracks.android.injection.components.AppComponent;
import org.owntracks.android.injection.components.DaggerAppComponent;
import org.owntracks.android.injection.modules.AppModule;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.services.LocationService;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.services.Scheduler;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import timber.log.Timber;

public class App extends Application  {
    public static final int MODE_ID_MQTT_PRIVATE =0;
    public static final int MODE_ID_MQTT_PUBLIC =2;
    public static final int MODE_ID_HTTP_PRIVATE = 3;

    private static SimpleDateFormat dateFormater;
    private static SimpleDateFormat dateFormaterToday;

    private static Handler mainHandler;
    private static Handler backgroundHandler;

    private Activity currentActivity;
    private static boolean inForeground;
    private int runningActivities = 0;

    private static AppComponent sAppComponent = null;


    @Override
	public void onCreate() {
		super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree() {
                @Override
                protected String createStackElementTag(StackTraceElement element) {
                    return super.createStackElementTag(element) + "/" + element.getMethodName() + "/" + element.getLineNumber();
                }
            });
        }
        sAppComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();

        dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateFormaterToday = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        HandlerThread mServiceHandlerThread = new HandlerThread("ServiceThread");
        mServiceHandlerThread.start();

        backgroundHandler = new Handler(mServiceHandlerThread.getLooper());
        mainHandler = new Handler(getMainLooper());

        checkFirstStart();
        getPreferences().initialize();

        postOnBackgroundHandler(new Runnable() {
            @Override
            public void run() {
                getMessageProcessor().initialize();
                startService(new Intent(getApplicationContext(), LocationService.class));

                //startService(new Intent(getApplicationContext(), ServiceProxy.class));
            }
        });
    }

    public static AppComponent getAppComponent() { return sAppComponent; }

    public static GeocodingProvider getGeocodingProvider() { return sAppComponent.geocodingProvider(); }

    public static ContactImageProvider getContactImageProvider() { return sAppComponent.contactImageProvider(); }

    public static Parser getParser() { return sAppComponent.parser(); }

    public static EventBus getEventBus() {
        return sAppComponent.eventBus();
    }

    public static Scheduler getScheduler() {
        return sAppComponent.scheduler();
    }

    public static ContactsRepo getContactsRepo() { return sAppComponent.contactsRepo(); }

    public static MessageProcessor getMessageProcessor() { return sAppComponent.messageProcessor(); }

    public static Preferences getPreferences() { return sAppComponent.preferences(); }


    public static Dao getDao() {
        return sAppComponent.dao();
    }

    public void enableForegroundBackgroundDetection() {
        registerActivityLifecycleCallbacks(new LifecycleCallbacks());
        registerScreenOnReceiver();
    }

    public static Context getContext() {
		return sAppComponent.context();
	}

    public static FusedContact getFusedContact(String topic) {
        return sAppComponent.contactsRepo().getById(topic);
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

	public static String formatDate(@NonNull Date d) {
        if(DateUtils.isToday(d.getTime())) {
            return dateFormaterToday.format(d);
        } else {
            return dateFormater.format(d);
        }
	}

	public static int getBatteryLevel() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = getContext().registerReceiver(null, ifilter);
		return batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : 0;
	}


    private void onEnterForeground() {
        inForeground = true;
        getMessageProcessor().onEnterForeground();

        ServiceProxy.onEnterForeground();

        Intent mIntent = new Intent(this, LocationService.class);
        mIntent.setAction(LocationService.INTENT_ACTION_CHANGE_BG);
        startService(mIntent);
    }

    private void onEnterBackground() {
        inForeground = false;
        ServiceProxy.onEnterBackground();

        Intent mIntent = new Intent(this, LocationService.class);
        mIntent.setAction(LocationService.INTENT_ACTION_CHANGE_BG);
        startService(mIntent);

    }

    public static boolean isInForeground() {
        return inForeground;
    }

    public static void restart() {
        Intent intent = new Intent(getContext(), ActivityWelcome.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    /*
     * Keeps track of running activities and if the app is in running in the foreground or background
     */
    private final class LifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        public void onActivityStarted(Activity activity) {

            runningActivities++;
            currentActivity = activity;
            if (runningActivities == 1) onEnterForeground();
        }

        public void onActivityStopped(Activity activity) {
            runningActivities--;
            if(currentActivity == activity)  currentActivity = null;
            if (runningActivities == 0) onEnterBackground();
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
                if ((strAction.equals(Intent.ACTION_SCREEN_OFF) || strAction.equals(Intent.ACTION_SCREEN_ON)) && isInForeground())
                {
                        onEnterBackground();
                }
            }
        };

        getApplicationContext().registerReceiver(screenOnOffReceiver, theFilter);

    }

    // Checks if the app is started for the first time.
    // On every new install this returns true for the first time and false afterwards
    private void checkFirstStart() {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);

        if(p.getBoolean(Preferences.Keys._FIRST_START, true)) {
            Timber.v("Initial application launch");
            String uuid = UUID.randomUUID().toString().toUpperCase();

            p.edit().putBoolean(Preferences.Keys._FIRST_START, false).putBoolean(Preferences.Keys._SETUP_NOT_COMPLETED , true).putString(Preferences.Keys._DEVICE_UUID, "A"+uuid.substring(1)).apply();

        }
    }

}
