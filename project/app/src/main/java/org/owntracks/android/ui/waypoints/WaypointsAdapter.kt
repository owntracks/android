package org.owntracks.android.ui.waypoints

import androidx.databinding.ViewDataBinding
import org.owntracks.android.BR
import org.owntracks.android.R
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.ui.base.BaseRecyclerViewAdapterWithClickHandler
import org.owntracks.android.ui.base.BaseRecyclerViewHolder

class WaypointsAdapter(clickListener: ClickListener<WaypointModel>) :
    BaseRecyclerViewAdapterWithClickHandler<
        WaypointModel, WaypointsAdapter.WaypointModelViewHolder>(
        clickListener, ::WaypointModelViewHolder, R.layout.ui_row_waypoint) {

  class WaypointModelViewHolder(binding: ViewDataBinding) :
      BaseRecyclerViewHolder<WaypointModel>(binding, BR.waypoint)
}
