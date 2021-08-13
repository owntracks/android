package org.owntracks.android.support

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunThingsOnOtherThreads @Inject constructor() {
    private val backgroundHandler: Handler
    private val networkHandler: Handler

    init {
        val serviceHandlerThread = HandlerThread(BACKGROUND_HANDLER_THREAD_NAME)
        serviceHandlerThread.start()

        val networkHandlerThread = HandlerThread(NETWORK_HANDLER_THREAD_NAME)
        networkHandlerThread.start()

        networkHandler = Handler(networkHandlerThread.looper)
        backgroundHandler = Handler(serviceHandlerThread.looper)
    }

    fun getBackgroundLooper(): Looper {
        return backgroundHandler.looper
    }

    fun postOnNetworkHandlerDelayed(r: Runnable, delayMilliseconds: Long) {
        networkHandler.postDelayed(r, delayMilliseconds)
    }

    fun postOnServiceHandlerDelayed(r: Runnable, delayMilliseconds: Long = 0) {
        backgroundHandler.postDelayed(r, delayMilliseconds)
    }

    companion object {
        const val NETWORK_HANDLER_THREAD_NAME = "networkHandlerThread"
        const val BACKGROUND_HANDLER_THREAD_NAME = "backgroundHandlerThread"
    }
}