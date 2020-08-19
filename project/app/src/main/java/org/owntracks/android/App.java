package org.owntracks.android;

import android.app.Application;
import android.content.Intent;
import android.os.StrictMode;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.injection.components.AppComponent;
import org.owntracks.android.injection.components.AppComponentProvider;
import org.owntracks.android.injection.components.DaggerAppComponent;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.RunThingsOnOtherThreads;
import org.owntracks.android.support.TimberDebugLogTree;
import org.owntracks.android.ui.map.MapActivity;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;
import timber.log.Timber;

public class App extends DaggerApplication  {
    private static Application sApplication;

    @Inject
    Preferences preferences;

    @Inject
    RunThingsOnOtherThreads runThingsOnOtherThreads;

    @Inject
    MessageProcessor messageProcessor;

    @Inject
    EventBus eventBus;

    @Override
    public void onCreate() {
        sApplication = this;
        WorkManager.initialize(this, new Configuration.Builder().build());

        super.onCreate();

        if(BuildConfig.DEBUG) {
            Timber.plant(new TimberDebugLogTree());
            Timber.e("StrictMode enabled in DEBUG build");
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyFlashScreen()
                    .penaltyDialog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectFileUriExposure()
                    .penaltyLog()
                    .build());

        } else {
            Timber.plant(new Timber.DebugTree());
        }
        for(Timber.Tree t : Timber.forest()) {
                Timber.v("Planted trees :%s", t);
        }

        preferences.checkFirstStart();

        // Running this on a background thread will deadlock FirebaseJobDispatcher.
        // Initialize will call Scheduler to connect off the main thread anyway.
        runThingsOnOtherThreads.postOnMainHandlerDelayed(() -> messageProcessor.initialize(), 510);
        eventBus.register(this);
    }

    @Subscribe
    public void onEvent(Events.RestartApp e) {
        Intent intent = new Intent(this.getApplicationContext(), MapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.getApplicationContext().startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        AppComponent appComponent = DaggerAppComponent.builder().app(this).build();
        appComponent.inject(this);
        AppComponentProvider.setAppComponent(appComponent);
        return appComponent;
    }
}