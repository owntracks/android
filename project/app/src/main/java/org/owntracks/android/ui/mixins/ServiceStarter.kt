package org.owntracks.android.ui.mixins

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import org.owntracks.android.services.BackgroundService
import timber.log.Timber

/**
 * Provides a mixin for Activities that want to be able to start the service
 */
interface ServiceStarter {
    fun startService(context: Context, action: String? = null, intent: Intent? = null)
    class Impl : ServiceStarter {
        override fun startService(context: Context, action: String?, intent: Intent?) {
            Timber.d("starting service")
            ContextCompat.startForegroundService(
                context,
                (intent ?: Intent()).setClass(context, BackgroundService::class.java)
                    .apply { action?.also { this.action = it } }
            )
        }
    }
}
