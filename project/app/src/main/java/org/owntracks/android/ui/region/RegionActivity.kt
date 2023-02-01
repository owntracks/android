package org.owntracks.android.ui.region

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiRegionBinding

@AndroidEntryPoint
class RegionActivity : AppCompatActivity() {
    private var saveButton: MenuItem? = null
    private val viewModel: RegionViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DataBindingUtil.setContentView<UiRegionBinding>(this, R.layout.ui_region).apply{
            vm = viewModel
            lifecycleOwner = this@RegionActivity
            setSupportActionBar(appbar.toolbar)
            description.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    conditionallyEnableSaveButton()
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
        conditionallyEnableSaveButton()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save -> {
                viewModel.saveWaypoint()
                finish()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun conditionallyEnableSaveButton() {
        saveButton?.run {
            isEnabled = viewModel.canSaveWaypoint()
            icon?.alpha = if (isEnabled) 255 else 130
        }
    }
}
