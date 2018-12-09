package org.owntracks.android;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.owntracks.android.injection.components.AppComponent;
import org.owntracks.android.injection.components.AppComponentProvider;
import org.owntracks.android.injection.components.DaggerAppComponent;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Runner;
import org.owntracks.android.ui.map.MapActivity;

import javax.inject.Inject;

import androidx.work.Configuration;
import androidx.work.WorkManager;
import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;
import timber.log.Timber;

public class App extends DaggerApplication  {
    private static App sInstance;

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
        WorkManager.initialize(this, new Configuration.Builder().build());

        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree() {
                @Override
                protected String createStackElementTag(@NonNull StackTraceElement element) {
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

    public static void restart() {
        Intent intent = new Intent(App.getInstance().getApplicationContext(), MapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        App.getInstance().getApplicationContext().startActivity(intent);
        Runtime.getRuntime().exit(0);
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