package org.owntracks.android.ui.waypoint

import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import androidx.databinding.BindingConversion
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.time.Instant
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWaypointBinding
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude
import org.owntracks.android.location.roundForDisplay

@AndroidEntryPoint
class WaypointActivity : AppCompatActivity() {
  private var saveButton: MenuItem? = null
  private var deleteButton: MenuItem? = null
  private val viewModel: WaypointViewModel by viewModels()
  private lateinit var binding: UiWaypointBinding
  private lateinit var textFields: List<TextInputEditText>

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    binding =
        DataBindingUtil.setContentView<UiWaypointBinding>(this, R.layout.ui_waypoint).apply {
          textFields = listOf(description, radius, latitude, longitude)
          vm = viewModel
          lifecycleOwner = this@WaypointActivity
          setSupportActionBar(appbar.toolbar)
          latitude.addTextChangedListener {
            it.toString().toDoubleOrNull()
                ?: run { latitude.error = getString(R.string.invalidLatitudeError) }
          }
          longitude.addTextChangedListener {
            it.toString().toDoubleOrNull()
                ?: run { longitude.error = getString(R.string.invalidLongitudeError) }
          }
          textFields.forEach { it.addTextChangedListener { setSaveButtonEnabledStatus() } }

          // Handle window insets for edge-to-edge
          ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            appbar.root.updatePadding(top = insets.top)
            WindowInsetsCompat.CONSUMED
          }
        }

    supportActionBar?.run {
      setDisplayShowHomeEnabled(true)
      setDisplayHomeAsUpEnabled(true)
    }

    if (intent.hasExtra("waypointId")) {
      viewModel.loadWaypoint(intent.getLongExtra("waypointId", 0))
      viewModel.waypoint.observe(this) { setDeleteButtonEnabledStatus() }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.activity_waypoint, menu)
    saveButton = menu.findItem(R.id.save)
    deleteButton = menu.findItem(R.id.delete)
    setSaveButtonEnabledStatus()
    setDeleteButtonEnabledStatus()
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.save -> {
        viewModel.saveWaypoint(
            binding.description.text.toString(),
            Latitude(binding.latitude.text.toString().toDouble()),
            Longitude(binding.longitude.text.toString().toDouble()),
            binding.radius.text.toString().toIntOrNull() ?: 1)
        finish()
        true
      }
      R.id.delete -> {
        MaterialAlertDialogBuilder(this) // set message, title, and icon
            .setTitle(R.string.deleteWaypointTitle)
            .setMessage(R.string.deleteWaypointConfirmationText)
            .setPositiveButton(R.string.deleteWaypointConfirmationButtonLabel) {
                dialog: DialogInterface,
                _: Int ->
              viewModel.delete()
              dialog.dismiss()
              finish()
            }
            .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int ->
              dialog.dismiss()
            }
            .create()
            .show()
        true
      }
      android.R.id.home -> {
        finish()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun setSaveButtonEnabledStatus() =
      saveButton?.run {
        isEnabled = !textFields.any { it.text.isNullOrBlank() || it.error != null }
        icon?.alpha = if (isEnabled) 255 else 130
      }

  private fun setDeleteButtonEnabledStatus() =
      deleteButton?.apply {
        isEnabled = viewModel.canDeleteWaypoint()
        icon?.alpha = if (isEnabled) 255 else 130
      }
}

@BindingAdapter("relativeTimeSpanString")
fun TextView.setRelativeTimeSpanString(instant: Instant?) {
  text =
      if (instant == null || instant == Instant.MIN) {
        ""
      } else if (DateUtils.isToday(instant.toEpochMilli())) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(instant.toEpochMilli())
      } else {
        DateFormat.getDateInstance(DateFormat.SHORT).format(instant.toEpochMilli())
      }
}

@BindingAdapter("relativeTimeSpanString")
fun TextView.setRelativeTimeSpanString(epochSeconds: Long?) {
  val instant = epochSeconds?.run(Instant::ofEpochSecond) ?: Instant.MIN
  text =
      if (instant == Instant.MIN) {
        ""
      } else if (DateUtils.isToday(instant.toEpochMilli())) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(instant.toEpochMilli())
      } else {
        DateFormat.getDateInstance(DateFormat.SHORT).format(instant.toEpochMilli())
      }
}

@BindingAdapter("android:text")
fun TextInputEditText.setLatitude(latitude: Latitude) {
  setText(latitude.value.roundForDisplay())
}

@BindingAdapter("android:text")
fun TextInputEditText.setLongitude(longitude: Longitude) {
  setText(longitude.value.roundForDisplay())
}

@BindingConversion fun fromStringToLatitude(value: String): Latitude = Latitude(value.toDouble())
