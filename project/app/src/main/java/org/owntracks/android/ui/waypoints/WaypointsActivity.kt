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
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.ComparableTimeMark
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.databinding.UiWaypointsBinding
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.DrawerProvider
import org.owntracks.android.test.CountingIdlingResourceShim
import org.owntracks.android.test.SimpleIdlingResource
import org.owntracks.android.ui.NotificationsStash
import org.owntracks.android.ui.base.BaseRecyclerViewAdapterWithClickHandler
import org.owntracks.android.ui.base.ClickHasBeenHandled
import org.owntracks.android.ui.base.RecyclerViewLayoutCompleteListener
import org.owntracks.android.ui.mixins.NotificationsPermissionRequested
import org.owntracks.android.ui.preferences.load.LoadActivity
import org.owntracks.android.ui.waypoint.WaypointActivity
import timber.log.Timber

@OptIn(ExperimentalTime::class)
@AndroidEntryPoint
class WaypointsActivity :
    AppCompatActivity(),
    BaseRecyclerViewAdapterWithClickHandler.ClickListener<WaypointModel>,
    RecyclerViewLayoutCompleteListener.RecyclerViewIdlingCallback,
    NotificationsPermissionRequested by NotificationsPermissionRequested.Impl() {
  private var recyclerViewStartLayoutInstant: ComparableTimeMark? = null
  private var layoutCompleteListener: RecyclerViewLayoutCompleteListener? = null

  @Inject lateinit var notificationsStash: NotificationsStash

  @Inject lateinit var drawerProvider: DrawerProvider

  @Inject lateinit var preferences: Preferences

  @Inject
  @Named("outgoingQueueIdlingResource")
  @get:VisibleForTesting
  lateinit var outgoingQueueIdlingResource: CountingIdlingResourceShim

  @Inject
  @Named("publishResponseMessageIdlingResource")
  @get:VisibleForTesting
  lateinit var publishResponseMessageIdlingResource: SimpleIdlingResource

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
          Timber.v("global layout changed")
          if (recyclerViewStartLayoutInstant != null) {
            this@WaypointsActivity.recyclerViewStartLayoutInstant?.run {
              Timber.d("Completed waypoints layout in ${this.elapsedNow()}")
            }
            this@WaypointsActivity.recyclerViewStartLayoutInstant = null
          }
          layoutCompleteListener?.run { onLayoutCompleted() }
        }
      }
    }
    lifecycleScope.launch {
      viewModel.waypointsList.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect {
        recyclerViewStartLayoutInstant = TimeSource.Monotonic.markNow()
        recyclerViewAdapter.setData(it)
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

  override fun setRecyclerViewLayoutCompleteListener(listener: RecyclerViewLayoutCompleteListener) {
    this.layoutCompleteListener = listener
  }

  override fun removeRecyclerViewLayoutCompleteListener(
      listener: RecyclerViewLayoutCompleteListener
  ) {
    if (this.layoutCompleteListener == listener) {
      this.layoutCompleteListener = null
    }
  }

  override var isRecyclerViewLayoutCompleted: Boolean
    get() =
        (recyclerViewStartLayoutInstant == null).also {
          Timber.v("Being asked if I'm idle, saying $it")
        }
    set(value) {
      recyclerViewStartLayoutInstant = if (!value) TimeSource.Monotonic.markNow() else null
    }
}
