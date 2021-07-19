package org.owntracks.android.support

import android.content.Context
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.qualifiers.ApplicationContext
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.services.worker.Scheduler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRestarter @Inject constructor(
        @ApplicationContext private val applicationContext: Context,
        private val scheduler: Scheduler,
        private val messageProcessor: MessageProcessor
) {
    fun restart() {
        scheduler.cancelAllTasks()
        messageProcessor.stopSendingMessages()
        ProcessPhoenix.triggerRebirth(applicationContext)
    }
}