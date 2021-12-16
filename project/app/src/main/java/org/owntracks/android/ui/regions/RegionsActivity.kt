package org.owntracks.android.ui.regions

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.objectbox.android.AndroidScheduler
import io.objectbox.reactive.DataSubscription
import org.owntracks.android.R
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.databinding.UiRegionsBinding
import org.owntracks.android.ui.base.BaseActivity
import org.owntracks.android.ui.base.view.MvvmView
import org.owntracks.android.ui.region.RegionActivity

@AndroidEntryPoint
class RegionsActivity : BaseActivity<UiRegionsBinding, RegionsViewModel>(),
        RegionsAdapter.ClickListener, MvvmView {

    private var recyclerViewAdapter: RegionsAdapter? = null
    private var subscription: DataSubscription? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasEventBus(false)
        bindAndAttachContentView(R.layout.ui_regions, savedInstanceState)
        setSupportToolbar(binding.appbar.toolbar)
        setDrawer(binding.appbar.toolbar)

        recyclerViewAdapter = RegionsAdapter(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = recyclerViewAdapter
        binding.recyclerView.setEmptyView(binding.placeholder)
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

    override fun onClick(model: WaypointModel, view: View, longClick: Boolean) {
        if (longClick) {

            AlertDialog.Builder(this) //set message, title, and icon
                    .setTitle("Delete")
                    .setMessage("Do you want to Delete")
                    .setPositiveButton("Delete") { dialog: DialogInterface, _: Int ->
                        viewModel.delete(model)
                        dialog.dismiss()
                    }
                    .setNegativeButton("cancel") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                    .create()
                    .show()
        } else {
            val intent = Intent(this, RegionActivity::class.java)
            intent.putExtra("waypointId", model.tst)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        if (subscription == null || subscription!!.isCanceled) {
            subscription = viewModel.waypointsList.subscribe().on(AndroidScheduler.mainThread())
                    .observer(recyclerViewAdapter)
        }
    }

    override fun onStop() {
        super.onStop()
        subscription?.cancel()
    }
}