package org.owntracks.android.support

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import org.owntracks.android.injection.qualifier.AppContext
import org.owntracks.android.injection.scopes.PerApplication
import javax.inject.Inject

@PerApplication
class RunThingsOnOtherThreads @Inject constructor(@AppContext appContext: Context) {

    private val backgroundHandler: Handler
    private val mainHandler: Handler
    private val networkHandler: Handler


    init {
        val serviceHandlerThread = HandlerThread("backgroundHandlerThread")
        serviceHandlerThread.start()

        val networkHandlerThread = HandlerThread("networkHandlerThread")
        networkHandlerThread.start()

        networkHandler = Handler(networkHandlerThread.looper)
        backgroundHandler = Handler(serviceHandlerThread.looper)
        mainHandler = Handler(appContext.mainLooper)
    }
    fun getBackgroundLooper(): Looper {
        return backgroundHandler.looper
    }
    fun postOnMainHandlerDelayed(r: Runnable, delayMilliseconds: Long) {
        mainHandler.postDelayed(r, delayMilliseconds)
    }

    fun postOnBackgroundHandlerDelayed(r: Runnable, delayMilliseconds: Long) {
        backgroundHandler.postDelayed(r, delayMilliseconds)
    }

    fun postOnNetworkHandlerDelayed(r: Runnable, delayMilliseconds: Long) {
        networkHandler.postDelayed(r, delayMilliseconds)
    }
}