package org.owntracks.android.ui.preferences

import android.content.Context
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.owntracks.android.R

/** A preference with buttons to move it up or down a list */
class ReorderablePreference(
    context: Context,
    private val canMoveUp: Boolean,
    private val canMoveDown: Boolean,
    private val onMove: (offset: Int) -> Unit,
) : Preference(context) {
  init {
    widgetLayoutResource = R.layout.preference_widget_reorder
  }

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)
    bindButton(holder.findViewById(R.id.moveUp), canMoveUp, -1)
    bindButton(holder.findViewById(R.id.moveDown), canMoveDown, 1)
  }

  private fun bindButton(button: View, enabled: Boolean, offset: Int) {
    button.isEnabled = enabled
    // Keeps the buttons lined up, rather than collapsing the ones at either end of the list
    button.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
    button.setOnClickListener { onMove(offset) }
  }
}
