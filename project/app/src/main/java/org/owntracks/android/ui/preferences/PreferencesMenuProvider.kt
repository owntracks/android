package org.owntracks.android.ui.preferences

import android.app.Activity
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import org.owntracks.android.R
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.ui.status.StatusActivity

class PreferencesMenuProvider(private val context: Activity, private val messageProcessor: MessageProcessor) :
    MenuProvider {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.preferences_connection, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.connect -> {
                if (messageProcessor.isEndpointReady) {
                    messageProcessor.reconnect()
                }
                true
            }
            R.id.status -> {
                context.startActivity(Intent(this.context, StatusActivity::class.java))
                false
            }
            else -> {
                false
            }
        }
    }
}
