package org.owntracks.android.ui

import android.content.Context

interface AppShortcuts {
    fun enableLogViewerShortcut(applicationContext: Context)
    fun disableLogViewerShortcut(applicationContext: Context)
}