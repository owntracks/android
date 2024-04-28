package org.owntracks.android.support

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunThingsOnOtherThreads @Inject constructor(@ApplicationContext appContext: Context) {
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

  fun postOnMainHandlerDelayed(r: Runnable, delayMilliseconds: Long = 0) {
    mainHandler.postDelayed(r, delayMilliseconds)
  }

  fun postOnServiceHandlerDelayed(r: Runnable, delayMilliseconds: Long = 0) {
    backgroundHandler.postDelayed(r, delayMilliseconds)
  }

  companion object {
    const val NETWORK_HANDLER_THREAD_NAME = "networkHandlerThread"
    const val BACKGROUND_HANDLER_THREAD_NAME = "backgroundHandlerThread"
  }
}
