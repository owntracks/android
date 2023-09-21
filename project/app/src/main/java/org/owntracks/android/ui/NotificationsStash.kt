package org.owntracks.android.ui

import android.annotation.SuppressLint
import android.app.Notification
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A place to stash notifications that we've not been allowed to show, until the permission is granted for us to show them.
 *
 * @constructor Create empty Notifications stash
 */
@Singleton
class NotificationsStash @Inject constructor() {
    private val notifications = mutableListOf<Notification>()
    fun add(notification: Notification) {
        notifications.add(notification)
    }

    @SuppressLint("MissingPermission")
    fun showAll(from: NotificationManagerCompat) {
        notifications.run {
            forEach { from.notify(System.currentTimeMillis().toInt(), it) }
            clear()
        }
    }
}
