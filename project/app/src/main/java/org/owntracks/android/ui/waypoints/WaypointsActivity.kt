package org.owntracks.android.ui.waypoints

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
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.databinding.UiWaypointsBinding
import org.owntracks.android.support.DrawerProvider
import org.owntracks.android.ui.base.BaseRecyclerViewAdapterWithClickHandler
import org.owntracks.android.ui.base.ClickHasBeenHandled
import org.owntracks.android.ui.preferences.load.LoadActivity
import org.owntracks.android.ui.waypoint.WaypointActivity

@AndroidEntryPoint
class WaypointsActivity : AppCompatActivity(), BaseRecyclerViewAdapterWithClickHandler.ClickListener<WaypointModel> {
    @Inject
    lateinit var drawerProvider: DrawerProvider

    private val viewModel: WaypointsViewModel by viewModels()
    private lateinit var recyclerViewAdapter: WaypointsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recyclerViewAdapter = WaypointsAdapter(this)
        DataBindingUtil.setContentView<UiWaypointsBinding>(this, R.layout.ui_waypoints)
            .apply {
                vm = viewModel
                lifecycleOwner = this@WaypointsActivity
                setSupportActionBar(appbar.toolbar)
                drawerProvider.attach(appbar.toolbar)
                waypointsRecyclerView.apply {
                    layoutManager = LinearLayoutManager(this@WaypointsActivity)
                    adapter = recyclerViewAdapter
                    emptyView = placeholder
                }
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
                startActivity(Intent(this, WaypointActivity::class.java))
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
        startActivity(Intent(this, WaypointActivity::class.java).putExtra("waypointId", thing.id))
        return true
    }
}
