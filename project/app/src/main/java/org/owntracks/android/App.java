package org.owntracks.android;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.db.Dao;
import org.owntracks.android.injection.components.AppComponent;
import org.owntracks.android.injection.components.DaggerAppComponent;
import org.owntracks.android.injection.modules.AppModule;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.services.BackgroundService;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.services.Scheduler;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.map.MapActivity;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

import timber.log.Timber;

public class App extends Application  {
    public static final int MODE_ID_MQTT_PRIVATE =0;
    public static final int MODE_ID_HTTP_PRIVATE = 3;

    private static SimpleDateFormat dateFormater;
    private static SimpleDateFormat dateFormaterToday;
    private static SimpleDateFormat dateFormaterDate;


    public static Handler getBackgroundHandler() {
        return getAppComponent().runner().getBackgroundHandler();
    }

    private Activity currentActivity;
    private static boolean inForeground;
    private int runningActivities = 0;

    private static AppComponent sAppComponent = null;

    public static PowerManager getPowerManager() {
        return PowerManager.class.cast(getContext().getSystemService(Context.POWER_SERVICE));
    }


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
        dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        dateFormaterToday = new SimpleDateFormat("HH:mm", Locale.getDefault());
        dateFormaterDate = new SimpleDateFormat("HH:mm", Locale.getDefault());


        checkFirstStart();
        //noinspection ResultOfMethodCallIgnored
        App.getPreferences().getModeId(); //Dirty hack to make sure preferences are initialized for all classes not using DI
        enableForegroundBackgroundDetection();
        App.postOnBackgroundHandler(new Runnable() {
            @Override
            public void run() {
                getMessageProcessor().initialize();
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

    public static MessageProcessor getMessageProcessor() { return sAppComponent.messageProcessor(); }

    public static Preferences getPreferences() { return sAppComponent.preferences(); }


    public static Dao getDao() {
        return sAppComponent.dao();
    }

    private void enableForegroundBackgroundDetection() {
        registerActivityLifecycleCallbacks(new LifecycleCallbacks());
        registerScreenOnReceiver();
    }

    public static Context getContext() {
		return sAppComponent.context();
	}

    public static FusedContact getFusedContact(String topic) {
        return sAppComponent.contactsRepo().getById(topic);
    }


    public static void removeMainHandlerRunnable(Runnable r) {
        getAppComponent().runner().removeMainHandlerRunnable(r);
    }
    public static void postOnMainHandlerDelayed(Runnable r, long delayMilis) {
        getAppComponent().runner().postOnMainHandlerDelayed(r, delayMilis);
    }

    public static void postOnBackgroundHandlerDelayed(Runnable r, long delayMilis) {
        getAppComponent().runner().postOnBackgroundHandlerDelayed(r, delayMilis);
    }

    private static void postOnBackgroundHandler(Runnable r) {
        getAppComponent().runner().postOnBackgroundHandlerDelayed(r, 1);
    }

    public static String formatDate(long tstSeconds) {
        return formatDate(new Date(TimeUnit.SECONDS.toMillis(tstSeconds)));
    }


    public static String formatDateShort(long tstSeconds) {
        Date d = new Date(TimeUnit.SECONDS.toMillis(tstSeconds));
        if(DateUtils.isToday(d.getTime())) {
            return dateFormaterToday.format(d);
        } else {
            return dateFormaterDate.format(d);

        }
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
        Timber.v("entering foreground");
        inForeground = true;
        App.postOnBackgroundHandler(new Runnable() {
            @Override
            public void run() {
                startBackgroundServiceCompat(getContext(), BackgroundService.INTENT_ACTION_CHANGE_BG);
                getMessageProcessor().onEnterForeground();
            }
        });

    }

    private void onEnterBackground() {
        Timber.v("entering background");
        inForeground = false;
        App.postOnBackgroundHandler(new Runnable() {
            @Override
            public void run() {
                startBackgroundServiceCompat(getContext(), BackgroundService.INTENT_ACTION_CHANGE_BG);
                getMessageProcessor().onEnterBackground();
            }
        });
    }

    public static boolean isInForeground() {
        return inForeground;
    }

    public static void restart() {
        Intent intent = new Intent(getContext(), MapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    public static void startBackgroundServiceCompat(final @NonNull Context context) {
        startBackgroundServiceCompat(context, (new Intent(context, BackgroundService.class)));
    }
    public static void startBackgroundServiceCompat(final @NonNull Context context, final @Nullable String action) {
        startBackgroundServiceCompat(context, (new Intent(context, BackgroundService.class)).setAction(action));
    }

    public static void startBackgroundServiceCompat(final @NonNull Context context, @NonNull final Intent intent) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent);
                } else {
                    context.startService(intent);
                }
            }
        };
        postOnBackgroundHandlerDelayed(r, 1000);
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
                Timber.v("screenOnOffReceiver intent received");
                if ((Intent.ACTION_SCREEN_OFF.equals(strAction) || Intent.ACTION_SCREEN_ON.equals(strAction)) && isInForeground())
                {
                        onEnterBackground();
                }
            }
        };

        registerReceiver(screenOnOffReceiver, theFilter);

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
