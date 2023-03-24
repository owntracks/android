package org.owntracks.android.ui.waypoint

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.time.Instant
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWaypointBinding

@AndroidEntryPoint
class WaypointActivity : AppCompatActivity() {
    private var saveButton: MenuItem? = null
    private var deleteButton: MenuItem? = null
    private val viewModel: WaypointViewModel by viewModels()
    private lateinit var binding: UiWaypointBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView<UiWaypointBinding>(this, R.layout.ui_waypoint)
            .apply {
                vm = viewModel
                lifecycleOwner = this@WaypointActivity
                setSupportActionBar(appbar.toolbar)
                description.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable) {
                        setSaveButtonEnabledStatus()
                    }
                })
            }

        supportActionBar?.run {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        if (intent.hasExtra("waypointId")) {
            viewModel.loadWaypoint(intent.getLongExtra("waypointId", 0))
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
                    binding.latitude.text.toString()
                        .toDouble(),
                    binding.longitude.text.toString()
                        .toDouble(),
                    binding.radius.text.toString()
                        .toIntOrNull() ?: 1
                )
                finish()
                true
            }
            R.id.delete -> {
                MaterialAlertDialogBuilder(this) // set message, title, and icon
                    .setTitle(R.string.deleteWaypointTitle)
                    .setMessage(R.string.deleteWaypointConfirmation)
                    .setPositiveButton(R.string.deleteWaypointTitle) { dialog: DialogInterface, _: Int ->
                        viewModel.delete()
                        dialog.dismiss()
                        finish()
                    }
                    .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
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
            isEnabled = viewModel.canSaveWaypoint()
            icon?.alpha = if (isEnabled) 255 else 130
        }

    private fun setDeleteButtonEnabledStatus() =
        deleteButton?.apply {
            isEnabled = viewModel.canDeleteWaypoint()
            icon?.alpha = if (isEnabled) 255 else 130
        }
}

@BindingAdapter("relativeTimeSpanString")
fun TextView.setRelativeTimeSpanString(instant: Instant) {
    text = if (instant == Instant.MIN) {
        ""
    } else if (DateUtils.isToday(instant.toEpochMilli())) {
        DateFormat.getTimeInstance(DateFormat.SHORT)
            .format(instant.toEpochMilli())
    } else {
        DateFormat.getDateInstance(DateFormat.SHORT)
            .format(instant.toEpochMilli())
    }
}

@BindingAdapter("relativeTimeSpanString")
fun TextView.setRelativeTimeSpanString(epochSeconds: Long) {
    val instant = Instant.ofEpochSecond(epochSeconds)
    text = if (instant == Instant.MIN) {
        ""
    } else if (DateUtils.isToday(instant.toEpochMilli())) {
        DateFormat.getTimeInstance(DateFormat.SHORT)
            .format(instant.toEpochMilli())
    } else {
        DateFormat.getDateInstance(DateFormat.SHORT)
            .format(instant.toEpochMilli())
    }
}
