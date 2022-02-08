package org.owntracks.android.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.owntracks.android.R
import org.owntracks.android.ui.status.logs.LogViewerActivity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppShortcutsImpl @Inject constructor() : AppShortcuts {
    private val logViewerId = "logs"
    override fun enableLogViewerShortcut(applicationContext: Context) {
        val success = ShortcutManagerCompat.pushDynamicShortcut(
            applicationContext,
            ShortcutInfoCompat.Builder(applicationContext, logViewerId)
                .setShortLabel(applicationContext.getString(R.string.viewLogs))
                .setIcon(
                    IconCompat.createWithResource(
                        applicationContext,
                        R.drawable.ic_outline_info_24
                    )
                )
                .setIntent(
                    Intent(
                        applicationContext,
                        LogViewerActivity::class.java
                    ).setAction("android.intent.action.MAIN")
                )
                .build()
        )
        Timber.d("Adding logViewer application shortcut. Success=$success")
    }

    override fun disableLogViewerShortcut(applicationContext: Context) {
        ShortcutManagerCompat.removeDynamicShortcuts(applicationContext, listOf(logViewerId))
        Timber.d("Removing logViewer application shortcut")
    }
}