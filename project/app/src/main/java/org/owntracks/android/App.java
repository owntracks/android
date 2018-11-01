package org.owntracks.android;

import org.owntracks.android.injection.components.AppComponentProvider;
import org.owntracks.android.injection.components.AppComponent;
import org.owntracks.android.injection.components.DaggerAppComponent;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.services.BackgroundService;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.support.DaggerWorkerFactory;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Runner;
import org.owntracks.android.ui.map.MapActivity;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import androidx.work.Configuration;
import androidx.work.WorkManager;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.DaggerApplication;
import timber.log.Timber;

public class App extends DaggerApplication  {
    private static App sInstance;

    @Inject
    DispatchingAndroidInjector<Activity> mActivityInjector;

    @Inject
    Preferences preferences;

    @Inject
    Runner runner;

    @Inject
    MessageProcessor messageProcessor;

    @Inject
    Parser parser; 

    @Inject
    @AppContext
    Context context;

    @Override
    public void onCreate() {
        sInstance = this;
        WorkManager.initialize(this.getApplicationContext(),new Configuration.Builder().setWorkerFactory(new DaggerWorkerFactory()).build());
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree() {
                @Override
                protected String createStackElementTag(StackTraceElement element) {
                    return super.createStackElementTag(element) + "/" + element.getMethodName() + "/" + element.getLineNumber();
                }
            });
        }

        preferences.checkFirstStart();

        // Running this on a background thread will deadlock FirebaseJobDispatcher.
        // Initialize will call Scheduler to connect off the main thread anyway.
        runner.postOnMainHandlerDelayed(new Runnable() {
            @Override
            public void run() {
                messageProcessor.initialize();
            }
        }, 510);

    }

    @Deprecated
    public static Context getContext() {
        return sInstance.getApplicationContext();
    }

    public static void onBootComplete() {
        if (getInstance().preferences.getAutostartOnBoot()) {
            getInstance().startBackgroundServiceCompat(getInstance().getApplicationContext());
        }
    }

    public static void restart() {
        Intent intent = new Intent(App.getInstance().getApplicationContext(), MapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        App.getInstance().getApplicationContext().startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    public void startBackgroundServiceCompat(final @NonNull Context context) {
        startBackgroundServiceCompat(context, (new Intent(context, BackgroundService.class)));
    }

    public void startBackgroundServiceCompat(final @NonNull Context context, final @Nullable String action) {
        startBackgroundServiceCompat(context, (new Intent(context, BackgroundService.class)).setAction(action));
    }

    public void startBackgroundServiceCompat(final @NonNull Context context, @NonNull final Intent intent) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent);
                } else {
                    context.startService(intent);
                }
            }
        };
        runner.postOnBackgroundHandlerDelayed(r, 1000);
    }


    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        AppComponent appComponent = DaggerAppComponent.builder().app(this).build();
        appComponent.inject(this);
        AppComponentProvider.setAppComponent(appComponent);
        return appComponent;
    }

    public static App getInstance() {
        return App.sInstance;
    }
}