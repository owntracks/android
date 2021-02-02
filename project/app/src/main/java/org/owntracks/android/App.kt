package org.owntracks.android

import android.content.Intent
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import org.conscrypt.Conscrypt
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.owntracks.android.injection.components.DaggerAppComponent
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.Events.RestartApp
import org.owntracks.android.support.Preferences
import org.owntracks.android.support.RunThingsOnOtherThreads
import org.owntracks.android.support.TimberDebugLogTree
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.security.Security
import javax.inject.Inject

class App : DaggerApplication() {
    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var runThingsOnOtherThreads: RunThingsOnOtherThreads

    @Inject
    lateinit var messageProcessor: MessageProcessor

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var workerFactory: WorkerFactory

    @Inject
    lateinit var scheduler: Scheduler

    override fun onCreate() {
        // Make sure we use Conscrypt for advanced TLS features on all devices.
        // X509ExtendedTrustManager not available pre-24, fall back to device. https://github.com/google/conscrypt/issues/603
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Security.insertProviderAt(Conscrypt.newProviderBuilder().provideTrustManager(true).build(), 1)
        } else {
            Security.insertProviderAt(Conscrypt.newProviderBuilder().provideTrustManager(false).build(), 1)
        }

        super.onCreate()
        WorkManager.initialize(this, Configuration.Builder().setWorkerFactory(workerFactory).build())
        scheduler.cancelAllTasks()
        if (BuildConfig.DEBUG) {
            Timber.plant(TimberDebugLogTree())
            Timber.e("StrictMode enabled in DEBUG build")
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyFlashScreen()
                    .penaltyDialog()
                    .build())
            StrictMode.setVmPolicy(VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectFileUriExposure()
                    .penaltyLog()
                    .build())
        } else {
            Timber.plant(DebugTree())
        }
        for (t in Timber.forest()) {
            Timber.v("Planted trees :%s", t)
        }
        preferences.checkFirstStart()

        // Running this on a background thread will deadlock FirebaseJobDispatcher.
        // Initialize will call Scheduler to connect off the main thread anyway.
        runThingsOnOtherThreads.postOnMainHandlerDelayed(Runnable { messageProcessor.initialize() }, 510)
        eventBus.register(this)
    }

    @Subscribe
    fun onEvent(@Suppress("UNUSED_PARAMETER") e: RestartApp?) {
        val intent = Intent(this.applicationContext, MapActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        this.applicationContext.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        val appComponent = DaggerAppComponent.builder().app(this).build()
        appComponent.inject(this)
        return appComponent
    }
}