package org.owntracks.android.support;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.injection.components.AppComponentProvider;

import java.lang.reflect.Constructor;

import androidx.work.Worker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import timber.log.Timber;

public class DaggerWorkerFactory extends WorkerFactory {


    @Nullable
    public Worker createWorker(@NonNull Context appContext, @NonNull String workerClassName, @NonNull WorkerParameters workerParameters) {
        try {
            Class<? extends Worker> workerKlass = Class.forName(workerClassName).asSubclass(Worker.class);
            Constructor<? extends Worker> constructor = workerKlass.getDeclaredConstructor(Context.class, WorkerParameters.class);
            Worker instance = constructor.newInstance(appContext,workerParameters);
            AppComponentProvider.getAppComponent().inject(instance);
            return instance;

        } catch (Throwable exeption) {
            Timber.e( exeption, "Could not instantiate %s", workerClassName);
            return null;
        }
    }
}