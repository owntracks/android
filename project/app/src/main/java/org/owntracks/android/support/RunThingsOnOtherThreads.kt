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
        val serviceHandlerThread = HandlerThread(BACKGROUND_HANDLER_THREAD_NAME)
        serviceHandlerThread.start()

        val networkHandlerThread = HandlerThread(NETWORK_HANDLER_THREAD_NAME)
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

    fun postOnNetworkHandlerDelayed(r: Runnable, delayMilliseconds: Long) {
        networkHandler.postDelayed(r, delayMilliseconds)
    }

    companion object {
        const val NETWORK_HANDLER_THREAD_NAME = "networkHandlerThread"
        const val BACKGROUND_HANDLER_THREAD_NAME = "backgroundHandlerThread"
    }
}