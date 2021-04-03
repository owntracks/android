package org.owntracks.android.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
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
import androidx.lifecycle.Observer
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.Behavior.DragCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.R
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.databinding.UiMapBinding
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.gms.location.toGMSLatLng
import org.owntracks.android.location.*
import org.owntracks.android.model.FusedContact
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.services.LocationProcessor
import org.owntracks.android.services.MessageProcessorEndpointHttp
import org.owntracks.android.support.ContactImageProvider
import org.owntracks.android.support.Events.PermissionGranted
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.support.RunThingsOnOtherThreads
import org.owntracks.android.support.widgets.BindingConversions
import org.owntracks.android.ui.base.BaseActivity
import org.owntracks.android.ui.base.navigator.Navigator
import org.owntracks.android.ui.welcome.WelcomeActivity
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt

class MapActivity : BaseActivity<UiMapBinding?, MapMvvm.ViewModel<MapMvvm.View?>?>(), MapMvvm.View, View.OnClickListener, View.OnLongClickListener, PopupMenu.OnMenuItemClickListener, OnMapReadyCallback, Observer<Any?> {
    private val markers: MutableMap<String, Marker?> = HashMap()
    private var googleMap: GoogleMap? = null
    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>? = null
    private var isMapReady = false
    private var mMenu: Menu? = null
    private var locationProviderClient: LocationProviderClient? = null
    var locationRepoUpdaterCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Timber.d("Foreground location result received: %s", locationResult)
            locationRepo!!.setCurrentLocation(locationResult.lastLocation)
            super.onLocationResult(locationResult)
        }
    }

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
        setSupportToolbar(binding!!.toolbar, false, true)
        setDrawer(binding!!.toolbar)

        // Workaround for Google Maps crash on Android 6
        try {
            binding!!.mapView.onCreate(savedInstanceState)
        } catch (e: Exception) {
            Timber.e(e, "Failed to bind map to view.")
            isMapReady = false
        }
        bottomSheetBehavior = BottomSheetBehavior.from(binding!!.bottomSheetLayout)
        binding!!.contactPeek.contactRow.setOnClickListener(this)
        binding!!.contactPeek.contactRow.setOnLongClickListener(this)
        binding!!.moreButton.setOnClickListener { v: View -> showPopupMenu(v) }
        setBottomSheetHidden()
        val appBarLayout = binding!!.appBarLayout
        val params = appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = AppBarLayout.Behavior()
        behavior.setDragCallback(object : DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                return false
            }
        })
        params.behavior = behavior
        viewModel!!.contact!!.observe(this, this)
        viewModel!!.bottomSheetHidden!!.observe(this, { o: Boolean? ->
            if (o == null || o) {
                setBottomSheetHidden()
            } else {
                setBottomSheetCollapsed()
            }
        })
        viewModel!!.center!!.observe(this, { o: LatLng? ->
            if (o != null) {
                updateCamera(o)
            }
        })
        checkAndRequestLocationPermissions()
        Timber.v("starting BackgroundService")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, BackgroundService::class.java))
        } else {
            startService(Intent(this, BackgroundService::class.java))
        }
        locationProviderClient = LocationServices.getLocationProviderClient(this)
    }

    private fun checkAndRequestLocationPermissions() {
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
                if (viewModel!!.hasLocation()) {
                    binding!!.distance.visibility = View.VISIBLE
                    binding!!.distanceLabel.visibility = View.VISIBLE
                    val distance = FloatArray(2)
                    Location.distanceBetween(viewModel!!.currentLocation!!.latitude, viewModel!!.currentLocation!!.longitude, fusedContact.latLng.latitude, fusedContact.latLng.longitude, distance)
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

    public override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        try {
            binding!!.mapView.onSaveInstanceState(bundle)
        } catch (ignored: Exception) {
            isMapReady = false
        }
    }

    public override fun onDestroy() {
        try {
            binding!!.mapView.onDestroy()
        } catch (ignored: Exception) {
            isMapReady = false
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        isMapReady = false
        try {
            binding!!.mapView.onResume()
            if (googleMap == null) {
                Timber.v("map not ready. Running initDelayed()")
                isMapReady = false
                initMapDelayed()
            } else {
                Timber.v("map ready. Running onMapReady()")
                isMapReady = true
                viewModel!!.onMapReady()
            }
        } catch (e: Exception) {
            Timber.e(e, "Not showing map due to crash in Google Maps library")
            isMapReady = false
        }
        handleIntentExtras(intent)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkAndRequestLocationPermissions()
        }
        locationProviderClient!!.requestLocationUpdates(
                LocationRequest()
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(TimeUnit.SECONDS.toMillis(5)),
                locationRepoUpdaterCallback,
                Looper.getMainLooper()
        )
        updateMonitoringModeMenu()
    }

    override fun onPause() {
        super.onPause()
        try {
            binding!!.mapView.onPause()
        } catch (e: Exception) {
            isMapReady = false
        }
        locationProviderClient!!.removeLocationUpdates(locationRepoUpdaterCallback)
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

    override fun onLowMemory() {
        super.onLowMemory()
        try {
            binding!!.mapView.onLowMemory()
        } catch (ignored: Exception) {
            isMapReady = false
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentExtras(intent)
        try {
            binding!!.mapView.onLowMemory()
        } catch (ignored: Exception) {
            isMapReady = false
        }
    }

    private fun initMapDelayed() {
        isMapReady = false
        runThingsOnOtherThreads!!.postOnMainHandlerDelayed({ initMap() }, 500)
    }

    private fun initMap() {
        isMapReady = false
        try {
            binding!!.mapView.getMapAsync(this)
        } catch (ignored: Exception) {
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_map, menu)
        mMenu = menu
        if (viewModel!!.hasLocation()) enableLocationMenus() else disableLocationMenus()
        updateMonitoringModeMenu()
        return true
    }

    override fun updateMonitoringModeMenu() {
        if (mMenu == null) {
            return
        }
        val item = mMenu!!.findItem(R.id.menu_monitoring)
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
        if (mMenu != null) {
            mMenu!!.findItem(R.id.menu_mylocation).setEnabled(false).icon.alpha = 128
            mMenu!!.findItem(R.id.menu_report).setEnabled(false).icon.alpha = 128
        }
    }

    override fun enableLocationMenus() {
        if (mMenu != null) {
            mMenu!!.findItem(R.id.menu_mylocation).setEnabled(true).icon.alpha = 255
            mMenu!!.findItem(R.id.menu_report).setEnabled(true).icon.alpha = 255
        }
    }

    // MAP CALLBACKS
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        this.googleMap!!.isIndoorEnabled = false
        this.googleMap!!.setLocationSource(viewModel!!.mapLocationSource)
        this.googleMap!!.isMyLocationEnabled = true
        this.googleMap!!.uiSettings.isMyLocationButtonEnabled = false
        this.googleMap!!.setOnMapClickListener(viewModel!!.onMapClickListener)
        this.googleMap!!.setOnCameraMoveStartedListener(viewModel!!.onMapCameraMoveStartedListener)
        this.googleMap!!.setOnMarkerClickListener(viewModel!!.onMarkerClickListener)
//        this.googleMap!!.setInfoWindowAdapter(object : InfoWindowAdapter {
//            override fun getInfoWindow(marker: Marker): View {
//                return null
//            }
//
//            override fun getInfoContents(marker: Marker): View {
//                return null
//            }
//        })
        isMapReady = true
        viewModel!!.onMenuCenterDeviceClicked()
        viewModel!!.onMapReady()
    }

    private fun updateCamera(latLng: LatLng) {
        if (isMapReady) googleMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_LEVEL_STREET.toFloat()))
    }

    override fun clearMarkers() {
        if (isMapReady) googleMap!!.clear()
        markers.clear()
    }

    override fun removeMarker(contact: FusedContact?) {
        if (contact == null) return
        val m = markers[contact.id]
        m?.remove()
    }

    override fun updateMarker(contact: FusedContact?) {
        if (contact == null || !contact.hasLocation() || !isMapReady) {
            Timber.v("unable to update marker. null:%s, location:%s, mapReady:%s", contact == null, contact == null || contact.hasLocation(), isMapReady)
            return
        }
        Timber.v("updating marker for contact: %s", contact.id)
        var marker = markers[contact.id]
        if (marker != null && marker.tag != null) {
            marker.position = contact.latLng.toGMSLatLng()
        } else {
            // If a marker has been removed, its tag will be null. Doing anything with it will make it explode
            if (marker != null) {
                markers.remove(contact.id)
            }
            marker = googleMap!!.addMarker(MarkerOptions().position(contact.latLng.toGMSLatLng()).anchor(0.5f, 0.5f).visible(false))
            marker.tag = contact.id
            markers[contact.id] = marker
        }
        GlobalScope.launch(Dispatchers.Main) {
            contactImageProvider?.run {
                val bitmap = BitmapDescriptorFactory.fromBitmap(getBitmapFromCache(contact))
                marker?.let {
                    it.setIcon(bitmap)
                    it.isVisible = true
                }
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.menu_navigate) {
            val c = viewModel!!.activeContact
            if (c != null && c.hasLocation()) {
                try {
                    val l = c.latLng.toGMSLatLng()
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + l.latitude + "," + l.longitude))
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
        if (mMenu != null) mMenu!!.close()
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
            eventBus!!.postSticky(PermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    @get:VisibleForTesting
    val locationIdlingResource: IdlingResource
        get() = binding!!.vm!!.locationIdlingResource

    @get:VisibleForTesting
    val outgoingQueueIdlingResource: IdlingResource
        get() = countingIdlingResource!!

    companion object {
        const val BUNDLE_KEY_CONTACT_ID = "BUNDLE_KEY_CONTACT_ID"
        private const val ZOOM_LEVEL_STREET: Long = 15
        private const val PERMISSIONS_REQUEST_CODE = 1
    }
}