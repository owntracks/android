package org.owntracks.android.ui.regions

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.databinding.UiRegionsBinding
import org.owntracks.android.support.DrawerProvider
import org.owntracks.android.ui.region.RegionActivity
import javax.inject.Inject

@AndroidEntryPoint
class RegionsActivity : AppCompatActivity(), RegionsAdapter.ClickListener {
    @Inject
    lateinit var drawerProvider: DrawerProvider
    private val viewModel: RegionsViewModel by viewModels()
    private lateinit var recyclerViewAdapter: RegionsAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: UiRegionsBinding = DataBindingUtil.setContentView(this, R.layout.ui_regions)
        binding.vm = viewModel
        binding.lifecycleOwner = this
        setSupportActionBar(binding.appbar.toolbar)
        drawerProvider.attach(binding.appbar.toolbar)

        recyclerViewAdapter = RegionsAdapter(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = recyclerViewAdapter
        binding.recyclerView.setEmptyView(binding.placeholder)

        viewModel.waypointsList.observe(this, recyclerViewAdapter::setData)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_waypoints, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add -> {
                this.startActivity(Intent(this, RegionActivity::class.java))
                true
            }
            R.id.exportWaypointsService -> {
                viewModel.exportWaypoints()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(`object`: WaypointModel, view: View, longClick: Boolean) {
        if (longClick) {
            AlertDialog.Builder(this) //set message, title, and icon
                    .setTitle("Delete")
                    .setMessage("Do you want to Delete")
                    .setPositiveButton("Delete") { dialog: DialogInterface, _: Int ->
                        viewModel.delete(`object`)
                        dialog.dismiss()
                    }
                    .setNegativeButton("cancel") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                    .create()
                    .show()
        } else {
            val intent = Intent(this, RegionActivity::class.java)
            intent.putExtra("waypointId", `object`.tst)
            startActivity(intent)
        }
    }
}