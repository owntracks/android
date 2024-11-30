package org.owntracks.android.ui.waypoints

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.databinding.UiWaypointsBinding
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.DrawerProvider
import org.owntracks.android.test.SimpleIdlingResource
import org.owntracks.android.test.ThresholdIdlingResourceInterface
import org.owntracks.android.ui.NotificationsStash
import org.owntracks.android.ui.base.ClickHasBeenHandled
import org.owntracks.android.ui.base.ClickListener
import org.owntracks.android.ui.mixins.NotificationsPermissionRequested
import org.owntracks.android.ui.preferences.load.LoadActivity
import org.owntracks.android.ui.waypoint.WaypointActivity
import timber.log.Timber

@AndroidEntryPoint
class WaypointsActivity :
    AppCompatActivity(),
    ClickListener<WaypointModel>,
    NotificationsPermissionRequested by NotificationsPermissionRequested.Impl() {
  private var recyclerViewStartLayoutInstant: ComparableTimeMark? = null

  @Inject lateinit var notificationsStash: NotificationsStash

  @Inject lateinit var drawerProvider: DrawerProvider

  @Inject lateinit var preferences: Preferences

  @Inject
  @Named("outgoingQueueIdlingResource")
  @get:VisibleForTesting
  lateinit var outgoingQueueIdlingResource: ThresholdIdlingResourceInterface

  @Inject
  @Named("publishResponseMessageIdlingResource")
  @get:VisibleForTesting
  lateinit var publishResponseMessageIdlingResource: SimpleIdlingResource

  @Inject
  @Named("waypointsRecyclerViewIdlingResource")
  lateinit var waypointsRecyclerViewIdlingResource: ThresholdIdlingResourceInterface

  private val viewModel: WaypointsViewModel by viewModels()
  private lateinit var recyclerViewAdapter: WaypointsAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    recyclerViewAdapter = WaypointsAdapter(this)
    postNotificationsPermissionInit(this, preferences, notificationsStash)
    DataBindingUtil.setContentView<UiWaypointsBinding>(this, R.layout.ui_waypoints).apply {
      vm = viewModel
      lifecycleOwner = this@WaypointsActivity
      setSupportActionBar(appbar.toolbar)
      drawerProvider.attach(appbar.toolbar)
      waypointsRecyclerView.apply {
        layoutManager = LinearLayoutManager(this@WaypointsActivity)
        adapter = recyclerViewAdapter
        emptyView = placeholder
        viewTreeObserver.addOnGlobalLayoutListener {
          Timber.d(
              "WaypointsActivity: RecyclerView layout took ${recyclerViewStartLayoutInstant!!.elapsedNow()} and has ${recyclerViewAdapter.itemCount} items")
          waypointsRecyclerViewIdlingResource.set(recyclerViewAdapter.itemCount)
        }
      }

      lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
          viewModel.waypointsFlow.collect {
            recyclerViewStartLayoutInstant = TimeSource.Monotonic.markNow()
            Timber.d("submitting ${it.size} waypoints to adapter")
            recyclerViewAdapter.submitList(it)
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    requestNotificationsPermission()
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

  override fun onClick(thing: WaypointModel, view: View, longClick: Boolean): ClickHasBeenHandled {
    startActivity(Intent(this, WaypointActivity::class.java).putExtra("waypointId", thing.id))
    return true
  }
}
