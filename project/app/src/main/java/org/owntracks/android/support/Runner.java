package org.owntracks.android.support;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import org.owntracks.android.injection.qualifier.AppContext;

public class Runner {
    private static Handler mainHandler;
    private static Handler backgroundHandler;

    public Runner(@AppContext Context appContext) {
        HandlerThread mServiceHandlerThread = new HandlerThread("backgroundHandlerThread");
        mServiceHandlerThread.start();

        backgroundHandler = new Handler(mServiceHandlerThread.getLooper());
        mainHandler = new Handler(appContext.getMainLooper());
    }
    public void removeMainHandlerRunnable(Runnable r) {
        mainHandler.removeCallbacks(r);
    }
    public void postOnMainHandlerDelayed(Runnable r, long delayMilis) {
        mainHandler.postDelayed(r, delayMilis);
    }

    public void postOnMainHandler(Runnable r) {
        mainHandler.post(r);
    }

    public void postOnBackgroundHandlerDelayed(Runnable r, long delayMilis) {
        backgroundHandler.postDelayed(r, delayMilis);
    }

    private void postOnBackgroundHandler(Runnable r) {
        backgroundHandler.post(r);
    }

    public Handler getBackgroundHandler() {
        return backgroundHandler;
    }
}
