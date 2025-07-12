package org.owntracks.android.ui.waypoints

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.datetime.Instant
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlinx.datetime.toKotlinInstant
import org.owntracks.android.R
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.databinding.UiRowWaypointBinding
import org.owntracks.android.location.geofencing.Geofence.Companion.GEOFENCE_TRANSITION_DWELL
import org.owntracks.android.location.geofencing.Geofence.Companion.GEOFENCE_TRANSITION_ENTER
import org.owntracks.android.location.geofencing.Geofence.Companion.GEOFENCE_TRANSITION_EXIT
import org.owntracks.android.ui.base.ClickListener

class WaypointsAdapter(private val clickListener: ClickListener<WaypointModel>) :
    ListAdapter<WaypointModel, WaypointsAdapter.WaypointViewHolder>(WAYPOINT_COMPARATOR) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaypointViewHolder {
    val binding = UiRowWaypointBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return WaypointViewHolder(binding, clickListener)
  }

  override fun onBindViewHolder(holder: WaypointViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  class WaypointViewHolder(
      private val binding: UiRowWaypointBinding,
      private val clickListener: ClickListener<WaypointModel>
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bind(waypoint: WaypointModel) {
      binding.apply {
        text.text =
            when (waypoint.lastTransition) {
              GEOFENCE_TRANSITION_ENTER,
              GEOFENCE_TRANSITION_DWELL ->
                  binding.root.context.getString(R.string.waypoint_region_inside)

              GEOFENCE_TRANSITION_EXIT ->
                  binding.root.context.getString(R.string.waypoint_region_outside)

              else -> binding.root.context.getString(R.string.waypoint_region_unknown)
            }
        meta.text = getDisplayTimestamp(waypoint.tst.toKotlinInstant())

        title.text = waypoint.description

        root.setOnClickListener { view -> clickListener.onClick(waypoint, view, false) }
        root.setOnLongClickListener { view -> clickListener.onClick(waypoint, view, true) }
      }
    }

    private fun getDisplayTimestamp(instant: Instant): String =
        if (DateUtils.isToday(instant.toEpochMilliseconds())) {

              DateTimeComponents.Format {
                hour()
                char(':')
                minute()
              }
            } else {
              DateTimeComponents.Format {
                year()
                char('-')
                monthNumber()
                char('-')
                dayOfMonth()
                char(' ')
                hour()
                char(':')
                minute()
              }
            }
            .run { instant.format(this) }
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
}
