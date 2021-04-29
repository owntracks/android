package org.owntracks.android.ui.map

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.BuildConfig.FLAVOR
import org.owntracks.android.R
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.databinding.UiMapBinding
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.gms.location.toGMSLatLng
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.location.LocationServices
import org.owntracks.android.location.LocationSource
import org.owntracks.android.model.FusedContact
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.services.BackgroundService.BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG
import org.owntracks.android.services.LocationProcessor
import org.owntracks.android.services.MessageProcessorEndpointHttp
import org.owntracks.android.support.ContactImageProvider
import org.owntracks.android.support.Events.PermissionGranted
import org.owntracks.android.support.Preferences.Companion.EXPERIMENTAL_FEATURE_USE_OSM_MAP
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.support.RunThingsOnOtherThreads
import org.owntracks.android.support.widgets.BindingConversions
import org.owntracks.android.ui.base.BaseActivity
import org.owntracks.android.ui.base.navigator.Navigator
import org.owntracks.android.ui.map.osm.OSMMapFragment
import org.owntracks.android.ui.welcome.WelcomeActivity
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToInt

class MapActivity : BaseActivity<UiMapBinding?, MapMvvm.ViewModel<MapMvvm.View?>?>(), MapMvvm.View, View.OnClickListener, View.OnLongClickListener, PopupMenu.OnMenuItemClickListener, Observer<Any?> {
    lateinit var locationLifecycleObserver: LocationLifecycleObserver
    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>? = null
    private var menu: Menu? = null
    private var locationProviderClient: LocationProviderClient? = null
    private lateinit var mapFragment: MapFragment

    private lateinit var mapLocationSource: LocationSource

    @JvmField
    @Inject
    var locationRepo: LocationRepo? = null

    @JvmField
    @Inject
    var runThingsOnOtherThreads: RunThingsOnOtherThreads? = null

    @JvmField
    @Inject
    var contactImageProvider: ContactImageProvider? = null

    @JvmField
    @Inject
    var eventBus: EventBus? = null

    @JvmField
    @Inject
    var geocoderProvider: GeocoderProvider? = null

    @JvmField
    @Inject
    var countingIdlingResource: CountingIdlingResource? = null

    @JvmField
    @Inject
    var navigator: Navigator? = null

    @JvmField
    @Inject
    var requirementsChecker: RequirementsChecker? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!preferences.isSetupCompleted) {
            navigator!!.startActivity(WelcomeActivity::class.java)
            finish()
        }
        bindAndAttachContentView(R.layout.ui_map, savedInstanceState, contactImageProvider)

        binding?.also {
            setSupportToolbar(it.toolbar, false, true)
            setDrawer(it.toolbar)
            bottomSheetBehavior = BottomSheetBehavior.from(it.bottomSheetLayout)
            it.contactPeek.contactRow.setOnClickListener(this)
            it.contactPeek.contactRow.setOnLongClickListener(this)
            it.moreButton.setOnClickListener { v: View -> showPopupMenu(v) }
            setBottomSheetHidden()

            // Need to set the appbar layout behaviour to be non-drag, so that we can drag the map
            val behavior = AppBarLayout.Behavior()
            behavior.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                    return false
                }
            })
            (it.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior = behavior
        }

        locationLifecycleObserver = LocationLifecycleObserver(activityResultRegistry)
        lifecycle.addObserver(locationLifecycleObserver)

        locationProviderClient = LocationServices.getLocationProviderClient(this, preferences)
        mapLocationSource = MapLocationSource(locationProviderClient!!, viewModel!!.mapLocationUpdateCallback)

        if (savedInstanceState == null) {
            mapFragment = getMapFragment()
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.mapFragment, mapFragment, "map")
            }
        } else {
            mapFragment = supportFragmentManager.findFragmentByTag("map") as MapFragment
        }

        // Watch various things that the viewModel owns
        viewModel?.also {
            it.contact.observe(this, this)
            it.bottomSheetHidden.observe(this, { o: Boolean? ->
                if (o == null || o) {
                    setBottomSheetHidden()
                } else {
                    setBottomSheetCollapsed()
                }
            })
            it.mapCenter.observe(this, { o: LatLng ->
                mapFragment.updateCamera(o)
            })
            it.currentLocation.observe(this, { latLng ->
                if (latLng == null) {
                    disableLocationMenus()
                } else {
                    enableLocationMenus()
                }
            })
        }

        Timber.d("starting BackgroundService")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, BackgroundService::class.java))
        } else {
            startService(Intent(this, BackgroundService::class.java))
        }

        // We've been started in the foreground, so cancel the background restriction notification
        NotificationManagerCompat.from(this).cancel(BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG, 0)
    }

    private fun getMapFragment() =
            if (preferences.isExperimentalFeatureEnabled(EXPERIMENTAL_FEATURE_USE_OSM_MAP)) {
                OSMMapFragment()
            } else {
                when (FLAVOR) {
                    "gms" -> GoogleMapFragment(mapLocationSource, locationRepo)
                    else -> OSMMapFragment()
                }
            }

    internal fun checkAndRequestLocationPermissions(): Boolean {
        if (!requirementsChecker!!.isPermissionCheckPassed()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    val currentActivity: Activity = this
                    AlertDialog.Builder(this)
                            .setCancelable(true)
                            .setMessage(R.string.permissions_description)
                            .setPositiveButton("OK"
                            ) { _: DialogInterface?, _: Int -> ActivityCompat.requestPermissions(currentActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_CODE) }
                            .show()
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_CODE)
                }
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_CODE)
            }
            return false
        } else {
            return true
        }
    }

    override fun onChanged(activeContact: Any?) {
        if (activeContact != null) {
            val fusedContact = activeContact as FusedContact
            Timber.v("for contact: %s", fusedContact.id)
            binding!!.contactPeek.name.text = fusedContact.fusedName
            if (fusedContact.hasLocation()) {
                GlobalScope.launch(Dispatchers.Main) {
                    contactImageProvider?.run {
                        binding!!.contactPeek.image.setImageBitmap(getBitmapFromCache(fusedContact))
                    }
                }
                geocoderProvider!!.resolve(fusedContact.messageLocation.value!!, binding!!.contactPeek.location)
                BindingConversions.setRelativeTimeSpanString(binding!!.contactPeek.locationDate, fusedContact.tst)
                binding!!.acc.text = String.format(Locale.getDefault(), "%s m", fusedContact.fusedLocationAccuracy)
                binding!!.tid.text = fusedContact.trackerId
                binding!!.id.text = fusedContact.id
                if (viewModel!!.currentLocation.value != null) {
                    binding!!.distance.visibility = View.VISIBLE
                    binding!!.distanceLabel.visibility = View.VISIBLE
                    val distance = FloatArray(2)
                    Location.distanceBetween(
                            viewModel!!.currentLocation.value!!.latitude,
                            viewModel!!.currentLocation.value!!.longitude,
                            fusedContact.latLng.latitude,
                            fusedContact.latLng.longitude,
                            distance)
                    binding!!.distance.text = String.format(Locale.getDefault(), "%d m", distance[0].roundToInt())
                } else {
                    binding!!.distance.visibility = View.GONE
                    binding!!.distanceLabel.visibility = View.GONE
                }
            } else {
                binding!!.contactPeek.location.setText(R.string.na)
                binding!!.contactPeek.locationDate.setText(R.string.na)
            }
        }
    }

    override fun onResume() {
        if (FLAVOR == "gms") {
            if (mapFragment is GoogleMapFragment && preferences.isExperimentalFeatureEnabled(EXPERIMENTAL_FEATURE_USE_OSM_MAP)) {
                mapFragment = OSMMapFragment()
                supportFragmentManager.commit(true) {
                    this.replace(R.id.mapFragment, mapFragment)
                }
            } else if (mapFragment is OSMMapFragment && !preferences.isExperimentalFeatureEnabled(EXPERIMENTAL_FEATURE_USE_OSM_MAP)) {
                mapFragment = GoogleMapFragment(mapLocationSource, locationRepo)
                supportFragmentManager.commit(true) {
                    this.replace(R.id.mapFragment, mapFragment)
                }
            }
        }
        super.onResume()
        handleIntentExtras(intent)
        updateMonitoringModeMenu()
    }

    private fun handleIntentExtras(intent: Intent) {
        Timber.v("handleIntentExtras")
        val b = navigator!!.getExtrasBundle(intent)
        if (b != null) {
            Timber.v("intent has extras from drawerProvider")
            val contactId = b.getString(BUNDLE_KEY_CONTACT_ID)
            if (contactId != null) {
                viewModel!!.restore(contactId)
            }
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentExtras(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_map, menu)
        this.menu = menu
        updateMonitoringModeMenu()
        return true
    }

    override fun updateMonitoringModeMenu() {
        if (menu == null) {
            return
        }
        val item = menu!!.findItem(R.id.menu_monitoring)
        when (preferences.monitoring) {
            LocationProcessor.MONITORING_QUIET -> {
                item.setIcon(R.drawable.ic_baseline_stop_36)
                item.setTitle(R.string.monitoring_quiet)
            }
            LocationProcessor.MONITORING_MANUAL -> {
                item.setIcon(R.drawable.ic_baseline_pause_36)
                item.setTitle(R.string.monitoring_manual)
            }
            LocationProcessor.MONITORING_SIGNIFICANT -> {
                item.setIcon(R.drawable.ic_baseline_play_arrow_36)
                item.setTitle(R.string.monitoring_significant)
            }
            LocationProcessor.MONITORING_MOVE -> {
                item.setIcon(R.drawable.ic_step_forward_2)
                item.setTitle(R.string.monitoring_move)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_report -> {
                viewModel!!.sendLocation()
                return true
            }
            R.id.menu_mylocation -> {
                viewModel!!.onMenuCenterDeviceClicked()
                return true
            }
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_monitoring -> {
                stepMonitoringModeMenu()
            }
        }
        return false
    }

    private fun stepMonitoringModeMenu() {
        preferences.setMonitoringNext()
        when (preferences.monitoring) {
            LocationProcessor.MONITORING_QUIET -> {
                Toast.makeText(this, R.string.monitoring_quiet, Toast.LENGTH_SHORT).show()
            }
            LocationProcessor.MONITORING_MANUAL -> {
                Toast.makeText(this, R.string.monitoring_manual, Toast.LENGTH_SHORT).show()
            }
            LocationProcessor.MONITORING_SIGNIFICANT -> {
                Toast.makeText(this, R.string.monitoring_significant, Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, R.string.monitoring_move, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disableLocationMenus() {
        if (menu != null) {
            menu!!.findItem(R.id.menu_mylocation).setEnabled(false).icon.alpha = 128
            menu!!.findItem(R.id.menu_report).setEnabled(false).icon.alpha = 128
        }
    }

    private fun enableLocationMenus() {
        if (menu != null) {
            menu!!.findItem(R.id.menu_mylocation).setEnabled(true).icon.alpha = 255
            menu!!.findItem(R.id.menu_report).setEnabled(true).icon.alpha = 255
        }
    }

    override fun clearMarkers() {
        mapFragment.clearMarkers()
    }

    override fun removeMarker(contact: FusedContact) {
        mapFragment.removeMarker(contact.id)
    }

    override fun updateMarker(contact: FusedContact) {
        if (!contact.hasLocation()) {
            Timber.i("unable to update marker. no location")
            return
        }
        Timber.v("updating marker for contact: %s", contact.id)
        mapFragment.updateMarker(contact.id, contact.latLng)
        GlobalScope.launch(Dispatchers.Main) {
            contactImageProvider?.run {
                getBitmapFromCache(contact)?.let {
                    mapFragment.setMarkerImage(contact.id, it)
                }
            }
        }
    }

    fun onMarkerClicked(id: String) {
        viewModel?.onMarkerClick(id)
    }

    fun onMapClick() {
        viewModel?.onMapClick()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.menu_navigate) {
            val c = viewModel!!.activeContact
            if (c != null && c.hasLocation()) {
                try {
                    val l = c.latLng.toGMSLatLng()
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${l.latitude},${l.longitude}"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, getString(R.string.noNavigationApp), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.contactLocationUnknown), Toast.LENGTH_SHORT).show()
            }
            return true
        } else if (itemId == R.id.menu_clear) {
            viewModel!!.onClearContactClicked()
            return false
        }
        return false
    }

    override fun onLongClick(view: View): Boolean {
        viewModel!!.onBottomSheetLongClick()
        return true
    }

    override fun setBottomSheetExpanded() {
        bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // BOTTOM SHEET CALLBACKS
    override fun onClick(view: View) {
        viewModel!!.onBottomSheetClick()
    }

    override fun setBottomSheetCollapsed() {
        Timber.v("vm contact: %s", binding!!.vm!!.activeContact)
        bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun setBottomSheetHidden() {
        bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
        if (menu != null) menu!!.close()
    }

    private fun showPopupMenu(v: View) {
        val popupMenu = PopupMenu(this, v, Gravity.START)
        popupMenu.menuInflater.inflate(R.menu.menu_popup_contacts, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener(this)
        if (preferences.mode == MessageProcessorEndpointHttp.MODE_ID) popupMenu.menu.removeItem(R.id.menu_clear)
        popupMenu.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mapFragment.locationPermissionGranted()
            eventBus!!.postSticky(PermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    fun onMapReady() {
        viewModel?.onMapReady()
    }

    @get:VisibleForTesting
    val locationIdlingResource: IdlingResource?
        get() = binding?.vm?.locationIdlingResource

    @get:VisibleForTesting
    val outgoingQueueIdlingResource: IdlingResource?
        get() = countingIdlingResource

    companion object {
        const val BUNDLE_KEY_CONTACT_ID = "BUNDLE_KEY_CONTACT_ID"
        const val STARTING_LATITUDE = 48.856826
        const val STARTING_LONGITUDE = 2.292713
        private const val PERMISSIONS_REQUEST_CODE = 1
    }
}