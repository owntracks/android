package org.owntracks.android.ui.waypoints

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.owntracks.android.R
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.databinding.UiRowWaypointBinding

class WaypointsAdapter :
    ListAdapter<WaypointModel, WaypointsAdapter.WaypointViewHolder>(WAYPOINT_COMPARATOR) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaypointViewHolder {
    val binding =
        DataBindingUtil.inflate<UiRowWaypointBinding>(
            LayoutInflater.from(parent.context), R.layout.ui_row_waypoint, parent, false)
    return WaypointViewHolder(binding)
  }

  override fun onBindViewHolder(holder: WaypointViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  class WaypointViewHolder(private val binding: UiRowWaypointBinding) :
      RecyclerView.ViewHolder(binding.root) {
    fun bind(waypoint: WaypointModel) {
      binding.waypoint = waypoint
      binding.executePendingBindings()
    }
  }

  companion object {
    private val WAYPOINT_COMPARATOR =
        object : DiffUtil.ItemCallback<WaypointModel>() {
          override fun areItemsTheSame(oldItem: WaypointModel, newItem: WaypointModel): Boolean {
            return oldItem.id == newItem.id
          }

          override fun areContentsTheSame(oldItem: WaypointModel, newItem: WaypointModel): Boolean {
            return oldItem == newItem
          }
        }
  }
}
