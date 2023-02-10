package org.owntracks.android.ui.regions

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.databinding.UiRegionsBinding
import org.owntracks.android.support.DrawerProvider
import org.owntracks.android.ui.base.BaseRecyclerViewAdapterWithClickHandler
import org.owntracks.android.ui.base.ClickHasBeenHandled
import org.owntracks.android.ui.preferences.load.LoadActivity
import org.owntracks.android.ui.region.RegionActivity

@AndroidEntryPoint
class RegionsActivity : AppCompatActivity(), BaseRecyclerViewAdapterWithClickHandler.ClickListener<WaypointModel> {
    @Inject
    lateinit var drawerProvider: DrawerProvider

    private val viewModel: RegionsViewModel by viewModels()
    private lateinit var recyclerViewAdapter: RegionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recyclerViewAdapter = RegionsAdapter(this)
        DataBindingUtil.setContentView<UiRegionsBinding>(this, R.layout.ui_regions)
            .apply {
                vm = viewModel
                lifecycleOwner = this@RegionsActivity
                setSupportActionBar(appbar.toolbar)
                drawerProvider.attach(appbar.toolbar)
                recyclerView.layoutManager = LinearLayoutManager(this@RegionsActivity)
                recyclerView.adapter = recyclerViewAdapter
                recyclerView.setEmptyView(placeholder)
            }
        viewModel.waypointsList.observe(this, recyclerViewAdapter::setData)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_waypoints, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add -> {
                startActivity(Intent(this, RegionActivity::class.java))
                true
            }
            R.id.exportWaypointsService -> {
                viewModel.exportWaypoints()
                true
            }
            R.id.importWaypoints -> {
                startActivity(Intent(this, LoadActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(
        thing: WaypointModel,
        view: View,
        longClick: Boolean
    ): ClickHasBeenHandled {
        val intent = Intent(this, RegionActivity::class.java)
        intent.putExtra("waypointId", thing.tst)
        startActivity(intent)
        return true
    }
}
