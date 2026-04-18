package org.owntracks.android.ui.preferences.load

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.owntracks.android.R

class ConfigItemAdapter : ListAdapter<ConfigItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

  override fun getItemViewType(position: Int): Int =
      when (getItem(position)) {
        is ConfigItem.Header -> VIEW_TYPE_HEADER
        is ConfigItem.KeyValue -> VIEW_TYPE_KEY_VALUE
        is ConfigItem.Waypoint -> VIEW_TYPE_WAYPOINT
        is ConfigItem.Summary -> VIEW_TYPE_SUMMARY
      }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      VIEW_TYPE_HEADER ->
          HeaderViewHolder(
              inflater.inflate(R.layout.ui_preferences_load_item_header, parent, false))
      VIEW_TYPE_KEY_VALUE ->
          KeyValueViewHolder(
              inflater.inflate(R.layout.ui_preferences_load_item_keyvalue, parent, false))
      VIEW_TYPE_WAYPOINT ->
          WaypointViewHolder(
              inflater.inflate(R.layout.ui_preferences_load_item_waypoint, parent, false))
      else ->
          SummaryViewHolder(
              inflater.inflate(R.layout.ui_preferences_load_item_summary, parent, false))
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (val item = getItem(position)) {
      is ConfigItem.Header -> (holder as HeaderViewHolder).bind(item)
      is ConfigItem.KeyValue -> (holder as KeyValueViewHolder).bind(item)
      is ConfigItem.Waypoint -> (holder as WaypointViewHolder).bind(item)
      is ConfigItem.Summary -> (holder as SummaryViewHolder).bind(item)
    }
  }

  class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val label: TextView = view.findViewById(R.id.headerLabel)

    fun bind(item: ConfigItem.Header) {
      label.text = label.context.getString(item.labelRes)
    }
  }

  class KeyValueViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val key: TextView = view.findViewById(R.id.preferenceKey)
    private val value: TextView = view.findViewById(R.id.preferenceValue)

    fun bind(item: ConfigItem.KeyValue) {
      key.text = if (item.labelRes != null) key.context.getString(item.labelRes) else item.key
      value.text =
          if (item.oldValue != null) "${item.oldValue} → ${item.newValue}" else item.newValue
    }
  }

  class SummaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val text: TextView = view.findViewById(R.id.summaryText)

    fun bind(item: ConfigItem.Summary) {
      text.text =
          text.resources.getQuantityString(
              R.plurals.loadActivityUnchangedSettingsCount, item.count, item.count)
    }
  }

  class WaypointViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val description: TextView = view.findViewById(R.id.waypointDescription)
    private val coordinates: TextView = view.findViewById(R.id.waypointCoordinates)

    fun bind(item: ConfigItem.Waypoint) {
      description.text = item.description
      coordinates.text = buildString {
        append("%.5f, %.5f".format(item.latitude, item.longitude))
        item.radius?.let { append(" · radius ${it}m") }
      }
    }
  }

  companion object {
    private const val VIEW_TYPE_HEADER = 0
    private const val VIEW_TYPE_KEY_VALUE = 1
    private const val VIEW_TYPE_WAYPOINT = 2
    private const val VIEW_TYPE_SUMMARY = 3

    private val DIFF_CALLBACK =
        object : DiffUtil.ItemCallback<ConfigItem>() {
          override fun areItemsTheSame(oldItem: ConfigItem, newItem: ConfigItem): Boolean =
              oldItem == newItem

          override fun areContentsTheSame(oldItem: ConfigItem, newItem: ConfigItem): Boolean =
              oldItem == newItem
        }
  }
}
