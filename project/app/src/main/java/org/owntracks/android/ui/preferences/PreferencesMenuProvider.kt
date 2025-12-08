package org.owntracks.android.ui.preferences

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.ui.status.StatusActivity

class PreferencesMenuProvider(
    private val context: Fragment,
    private val messageProcessor: MessageProcessor,
) : MenuProvider {
  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.preferences_connection, menu)
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    return when (menuItem.itemId) {
      R.id.connect -> {
        if (messageProcessor.isEndpointReady) {
          context.lifecycleScope.launch {
            Snackbar.make(
                    context.requireView(),
                    context.getString(R.string.reconnecting),
                    Snackbar.LENGTH_SHORT,
                )
                .show()

            val reconnectResult = messageProcessor.reconnect()
            reconnectResult
                .onSuccess {
                  Snackbar.make(
                          context.requireView(),
                          context.getString(R.string.CONNECTED),
                          Snackbar.LENGTH_SHORT,
                      )
                      .show()
                }
                .onFailure {
                  val errorMessage = reconnectResult.toString()
                  MaterialAlertDialogBuilder(context.requireContext())
                      .setCancelable(true)
                      .setIcon(R.drawable.ic_baseline_sync_problem_24)
                      .setTitle(context.getString(R.string.ERROR))
                      .setMessage(errorMessage)
                      .setPositiveButton(context.getString(R.string.copyText)) { dialog, _ ->
                        val clipboard =
                            context.requireContext().getSystemService(Context.CLIPBOARD_SERVICE)
                                as ClipboardManager
                        val clip = ClipData.newPlainText("ot_reconnect_error", errorMessage)
                        clipboard.setPrimaryClip(clip)
                        dialog.dismiss()
                      }
                      .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                      }
                      .show()
                }
          }
        }
        true
      }
      R.id.status -> {
        context.startActivity(Intent(this.context.requireActivity(), StatusActivity::class.java))
        false
      }
      else -> {
        false
      }
    }
  }
}
