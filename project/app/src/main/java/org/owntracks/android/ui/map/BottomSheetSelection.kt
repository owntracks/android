package org.owntracks.android.ui.map

import android.graphics.drawable.GradientDrawable
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import org.owntracks.android.R

/**
 * Emphasizes the currently-selected option in a bottom sheet picker (map layer style or monitoring
 * mode) by surrounding its button with a ring. The existing fill color is kept so the button
 * otherwise looks identical to the unselected options.
 */
fun AppCompatImageButton.showAsSelected() {
  val fillColor = backgroundTintList?.defaultColor ?: return
  background =
      GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fillColor)
        setStroke(
            resources.getDimensionPixelSize(R.dimen.bottom_sheet_selected_option_ring_width),
            ContextCompat.getColor(context, R.color.OTAccent),
        )
      }
}
