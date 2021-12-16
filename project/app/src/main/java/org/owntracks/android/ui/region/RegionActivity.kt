package org.owntracks.android.ui.region

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiRegionBinding
import org.owntracks.android.ui.base.BaseActivity
import org.owntracks.android.ui.base.view.MvvmView

@AndroidEntryPoint
class RegionActivity : BaseActivity<UiRegionBinding, RegionViewModel>(), MvvmView {
    private var saveButton: MenuItem? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasEventBus(false)
        bindAndAttachContentView(R.layout.ui_region, savedInstanceState)
        setSupportToolbar(binding.appbar.toolbar)

        if (intent.hasExtra("waypointId")) {
            viewModel.loadWaypoint(intent.getLongExtra("waypointId", 0))
        }

        binding.description.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                conditionallyEnableSaveButton()
            }
        })
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
            icon.alpha = if (isEnabled) 255 else 130
        }
    }
}