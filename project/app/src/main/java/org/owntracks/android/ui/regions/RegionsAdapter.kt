package org.owntracks.android.ui.regions

import android.view.View
import io.objectbox.reactive.DataObserver
import org.owntracks.android.BR
import org.owntracks.android.R
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.ui.base.BaseAdapter
import org.owntracks.android.ui.base.BaseAdapterItemView

internal class RegionsAdapter(clickListener: ClickListener?) : BaseAdapter<WaypointModel?>(
        BaseAdapterItemView.of(
                BR.waypoint, R.layout.ui_row_region
        )
), DataObserver<List<WaypointModel?>> {
    override fun onData(data: List<WaypointModel?>) {
        setItems(data)
    }

    internal interface ClickListener : BaseAdapter.ClickListener<WaypointModel?> {
        override fun onClick(`object`: WaypointModel, view: View, longClick: Boolean)
    }

    init {
        setClickListener(clickListener)
    }
}