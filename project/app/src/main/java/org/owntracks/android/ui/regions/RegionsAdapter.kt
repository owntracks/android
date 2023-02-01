package org.owntracks.android.ui.regions

import androidx.databinding.ViewDataBinding
import org.owntracks.android.BR
import org.owntracks.android.R
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.ui.base.BaseRecyclerViewAdapterWithClickHandler
import org.owntracks.android.ui.base.BaseRecyclerViewHolder

class RegionsAdapter(clickListener: ClickListener<WaypointModel>) :
    BaseRecyclerViewAdapterWithClickHandler<WaypointModel, RegionsAdapter.WaypointModelViewHolder>(
        clickListener, ::WaypointModelViewHolder, R.layout.ui_row_region
    ) {

    class WaypointModelViewHolder(binding: ViewDataBinding) :
        BaseRecyclerViewHolder<WaypointModel>(binding, BR.waypoint)
}
