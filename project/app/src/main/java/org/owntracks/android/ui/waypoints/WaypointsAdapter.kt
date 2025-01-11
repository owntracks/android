package org.owntracks.android.ui.waypoints

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.owntracks.android.R
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.databinding.UiRowWaypointBinding
import timber.log.Timber

class WaypointsAdapter :
    ListAdapter<WaypointModel, WaypointsAdapter.WaypointViewHolder>(WAYPOINT_COMPARATOR),
  AdapterView.OnItemClickListener {
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
            return oldItem.tst == newItem.tst
          }

          override fun areContentsTheSame(oldItem: WaypointModel, newItem: WaypointModel): Boolean {
            return oldItem.description == newItem.description &&
                oldItem.geofenceLatitude == newItem.geofenceLatitude &&
                oldItem.geofenceLongitude == newItem.geofenceLongitude &&
                oldItem.geofenceRadius == newItem.geofenceRadius
          }
        }
  }

  override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
    TODO("Not yet implemented")
  }
}
