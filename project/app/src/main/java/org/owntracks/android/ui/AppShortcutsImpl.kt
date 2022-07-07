package org.owntracks.android.ui

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.owntracks.android.R
import org.owntracks.android.ui.preferences.PreferencesActivity
import org.owntracks.android.ui.status.logs.LogViewerActivity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppShortcutsImpl @Inject constructor() : AppShortcuts {
    private val logViewerId = "logs"
    private val preferencesId = "preferences"
    override fun enableLogViewerShortcut(applicationContext: Context) {
        addshortCut(
            logViewerId,
            R.string.viewLogs,
            LogViewerActivity::class.java,
            applicationContext
        )
        addshortCut(
            preferencesId,
            R.string.title_activity_preferences,
            PreferencesActivity::class.java,
            applicationContext
        )
    }

    private fun addshortCut(
        id: String,
        @StringRes label: Int,
        activity: Class<*>,
        applicationContext: Context
    ) {
        val success = ShortcutManagerCompat.pushDynamicShortcut(
            applicationContext,
            ShortcutInfoCompat.Builder(applicationContext, id)
                .setShortLabel(applicationContext.getString(label))
                .setIcon(
                    IconCompat.createWithResource(
                        applicationContext,
                        R.drawable.ic_outline_info_24
                    )
                )
                .setIntent(
                    Intent(
                        applicationContext,
                        activity
                    ).setAction("android.intent.action.MAIN")
                )
                .build()
        )
        Timber.d("Adding logViewer application shortcut. Success=$success")
    }

    override fun disableLogViewerShortcut(applicationContext: Context) {
        ShortcutManagerCompat.removeDynamicShortcuts(
            applicationContext,
            listOf(logViewerId, preferencesId)
        )
        Timber.d("Removing logViewer application shortcut")
    }
}
