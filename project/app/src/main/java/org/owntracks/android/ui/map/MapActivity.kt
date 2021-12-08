package org.owntracks.android.ui.map

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.*
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.App
import org.owntracks.android.R
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.databinding.UiMapBinding
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.model.FusedContact
import org.owntracks.android.perfLog
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.services.BackgroundService.BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG
import org.owntracks.android.services.LocationProcessor
import org.owntracks.android.services.MessageProcessorEndpointHttp
import org.owntracks.android.support.ContactImageBindingAdapter
import org.owntracks.android.support.Preferences.Companion.EXPERIMENTAL_FEATURE_BEARING_ARROW_FOLLOWS_DEVICE_ORIENTATION
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.support.RunThingsOnOtherThreads
import org.owntracks.android.ui.base.BaseActivity
import org.owntracks.android.ui.base.navigator.Navigator
import org.owntracks.android.ui.welcome.WelcomeActivity
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MapActivity : BaseActivity<UiMapBinding?, MapMvvm.ViewModel<MapMvvm.View?>?>(), MapMvvm.View,
    View.OnClickListener, View.OnLongClickListener, PopupMenu.OnMenuItemClickListener {
    private var previouslyHadLocationPermissions: Boolean = false
    private var service: BackgroundService? = null
    lateinit var locationLifecycleObserver: LocationLifecycleObserver
    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayoutCompat>? = null
    private var menu: Menu? = null
    private lateinit var mapFragment: MapFragment
    private var sensorManager: SensorManager? = null
    private var orientationSensor: Sensor? = null

    internal lateinit var mapLocationSource: MapLocationSource
    private lateinit var locationServicesAlertDialog: AlertDialog
    private lateinit var locationPermissionsRationaleAlertDialog: AlertDialog

    @Inject
    lateinit var locationRepo: LocationRepo

    @Inject
    lateinit var runThingsOnOtherThreads: RunThingsOnOtherThreads

    @Inject
    lateinit var contactImageBindingAdapter: ContactImageBindingAdapter

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var geocoderProvider: GeocoderProvider

    @Inject
    lateinit var countingIdlingResource: CountingIdlingResource

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var requirementsChecker: RequirementsChecker

    @Inject
    lateinit var locationProviderClient: LocationProviderClient

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.d("Service connected to MapActivity")
            this@MapActivity.service = (service as BackgroundService.LocalBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.d("Service disconnected from MapActivity")
            service = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        perfLog {
            supportFragmentManager.fragmentFactory = EntryPointAccessors.fromActivity(
                this,
                MapActivityEntryPoint::class.java
            ).fragmentFactory
            super.onCreate(savedInstanceState)
            if (!preferences.isSetupCompleted) {
                navigator.startActivity(WelcomeActivity::class.java)
                finish()
            }
            bindAndAttachContentView(R.layout.ui_map, savedInstanceState)

            binding?.also {
                setSupportToolbar(it.appbar.toolbar, false, true)
                setDrawer(it.appbar.toolbar)
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
            }

            locationLifecycleObserver = LocationLifecycleObserver(activityResultRegistry)
            lifecycle.addObserver(locationLifecycleObserver)

            mapLocationSource =
                MapLocationSource(
                    locationProviderClient,
                    viewModel!!.mapLocationUpdateCallback
                )
            (supportFragmentManager.fragmentFactory as MapFragmentFactory).apply {mapLocationSource = this@MapActivity.mapLocationSource}

            // Watch various things that the viewModel owns
            viewModel?.also { vm ->
                vm.contact.observe(this, { contact: FusedContact? ->
                    contact?.let {
                        binding?.contactPeek?.run {
                            Timber.v("contact changed: $it.id")
                            name.text = it.fusedName
                            image.setImageResource(0)
                            GlobalScope.launch(Dispatchers.Main) {
                                contactImageBindingAdapter.run {
                                    image.setImageBitmap(
                                        getBitmapFromCache(it)
                                    )
                                }
                            }
                            vm.refreshGeocodeForActiveContact()
                        }
                    }
                })
                vm.bottomSheetHidden.observe(this, { o: Boolean? ->
                    if (o == null || o) {
                        setBottomSheetHidden()
                    } else {
                        setBottomSheetCollapsed()
                    }
                })
                vm.mapCenter.observe(this, { o: LatLng ->
                    if (::mapFragment.isInitialized) {
                        mapFragment.updateCamera(o)
                    }
                })
                vm.currentLocation.observe(this, { location ->
                    if (location == null) {
                        disableLocationMenus()
                    } else {
                        enableLocationMenus()
                        binding?.vm?.run {
                            updateActiveContactDistanceAndBearing(location)
                        }
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
            NotificationManagerCompat.from(this)
                .cancel(BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG, 0)

            (applicationContext as App).workManagerFailedToInitialize.observe(this, { value ->
                if (value) {
                    MaterialAlertDialogBuilder(this)
                        .setIcon(R.drawable.ic_baseline_warning_24)
                        .setTitle(getString(R.string.workmanagerInitializationErrorDialogTitle))
                        .setMessage(getString(R.string.workmanagerInitializationErrorDialogMessage))
                        .setPositiveButton(getString(R.string.workmanagerInitializationErrorDialogOpenSettingsLabel)) { _, _ ->
                            startActivity(
                                Intent(
                                    ACTION_APPLICATION_DETAILS_SETTINGS
                                ).apply {
                                    data = Uri.fromParts("package", packageName, "")
                                }
                            )
                        }
                        .setCancelable(true)
                        .show()
                }
            })
        }
    }

    internal fun checkAndRequestMyLocationCapability(explicitUserAction: Boolean): Boolean =
        checkAndRequestLocationPermissions(explicitUserAction) &&
                checkAndRequestLocationServicesEnabled(explicitUserAction)

    private val locationServicesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // We have to check permissions again here, because it may have been revoked in the
            // period between asking for location services and returning here.
            if (checkAndRequestLocationPermissions(false)) {
                mapFragment.myLocationEnabled()
            }
        }


    private fun checkAndRequestLocationServicesEnabled(explicitUserAction: Boolean): Boolean {
        return if (!requirementsChecker.isLocationServiceEnabled()) {
            Timber.d(Exception(), "Location Services disabled")
            if ((explicitUserAction || !preferences.userDeclinedEnableLocationServices)) {
                if (!this::locationServicesAlertDialog.isInitialized) {
                    locationServicesAlertDialog = MaterialAlertDialogBuilder(this)
                        .setCancelable(true)
                        .setIcon(R.drawable.ic_baseline_location_disabled_24)
                        .setTitle(getString(R.string.deviceLocationDisabledDialogTitle))
                        .setMessage(getString(R.string.deviceLocationDisabledDialogMessage))
                        .setPositiveButton(getString(R.string.deviceLocationDisabledDialogPositiveButtonLabel)) { _, _ ->
                            locationServicesLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            preferences.userDeclinedEnableLocationServices = true
                        }.create()
                }
                if (!locationServicesAlertDialog.isShowing) {
                    locationServicesAlertDialog.show()
                }
            }
            false
        } else {
            true
        }
    }

    private fun checkAndRequestLocationPermissions(explicitUserAction: Boolean): Boolean {
        if (!requirementsChecker.isLocationPermissionCheckPassed()) {
            Timber.d("No location permission")
            // We don't have location permission
            if ((explicitUserAction || !preferences.userDeclinedEnableLocationPermissions)) {
                // We should prompt for permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
                        // The user may have denied us once already, so show a rationale
                        if (!this::locationPermissionsRationaleAlertDialog.isInitialized) {
                            locationPermissionsRationaleAlertDialog =

                                MaterialAlertDialogBuilder(this)
                                    .setCancelable(true)
                                    .setIcon(R.drawable.ic_baseline_location_disabled_24)
                                    .setTitle(getString(R.string.locationPermissionRequestDialogTitle))
                                    .setMessage(R.string.locationPermissionRequestDialogMessage)
                                    .setPositiveButton(
                                        android.R.string.ok
                                    ) { _, _ ->
                                        ActivityCompat.requestPermissions(
                                            this,
                                            arrayOf(ACCESS_FINE_LOCATION),
                                            if (explicitUserAction) PERMISSIONS_REQUEST_CODE_WITH_EXPLICIT_SERVICES_CHECK else PERMISSIONS_REQUEST_CODE
                                        )
                                    }
                                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                                        preferences.userDeclinedEnableLocationPermissions = true
                                    }
                                    .create()
                        }
                        if (!locationPermissionsRationaleAlertDialog.isShowing) {
                            locationPermissionsRationaleAlertDialog.show()
                        }
                    } else {
                        // No need to show rationale, just request
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(ACCESS_FINE_LOCATION),
                            if (explicitUserAction) PERMISSIONS_REQUEST_CODE_WITH_EXPLICIT_SERVICES_CHECK else PERMISSIONS_REQUEST_CODE
                        )
                    }
                } else {
                    // Older android, so no rationale mech. Just request.
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(ACCESS_FINE_LOCATION),
                        if (explicitUserAction) PERMISSIONS_REQUEST_CODE_WITH_EXPLICIT_SERVICES_CHECK else PERMISSIONS_REQUEST_CODE
                    )
                }
            }
            return false
        } else {
            return true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Timber.d(
            "Permission result code=$requestCode. Permissions=${permissions.joinToString(",")} and grantResults=${
                grantResults.joinToString(
                    ","
                )
            }"
        )
        if (requestCode in setOf(
                PERMISSIONS_REQUEST_CODE,
                PERMISSIONS_REQUEST_CODE_WITH_EXPLICIT_SERVICES_CHECK
            )
            && grantResults.isNotEmpty()
            && permissions.isNotEmpty()
            && permissions.contains(ACCESS_FINE_LOCATION)
        ) {
            when (grantResults[permissions.indexOf(ACCESS_FINE_LOCATION)]) {
                PERMISSION_GRANTED -> {
                    if (requestCode == PERMISSIONS_REQUEST_CODE_WITH_EXPLICIT_SERVICES_CHECK) {
                        checkAndRequestLocationServicesEnabled(true)
                    }
                    mapFragment.myLocationEnabled()
                    service?.reInitializeLocationRequests()
                    previouslyHadLocationPermissions = true
                }
                PERMISSION_DENIED -> {
                    Snackbar.make(
                        binding!!.coordinatorLayout,
                        getString(R.string.locationPermissionNotGrantedNotification),
                        Snackbar.LENGTH_LONG
                    ).setAction(getString(R.string.fixProblemLabel)) {
                        startActivity(Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${packageName}")
                        })
                    }.show()
                }
            }
        }
    }


    override fun onResume() {
        mapFragment =
            supportFragmentManager.fragmentFactory.instantiate(this.classLoader, MapFragment::class.java.name) as MapFragment
        supportFragmentManager.commit(true) {
            replace(R.id.mapFragment, mapFragment, "map")
        }

        if (preferences.isExperimentalFeatureEnabled(
                EXPERIMENTAL_FEATURE_BEARING_ARROW_FOLLOWS_DEVICE_ORIENTATION
            )
        ) {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager?.let {
                orientationSensor = it.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                orientationSensor?.run { Timber.d("Got a rotation vector sensor") }
            }
        } else {
            sensorManager?.unregisterListener(viewModel?.orientationSensorEventListener)
            sensorManager = null
            orientationSensor = null
        }
        super.onResume()
        updateMonitoringModeMenu()
        updateMyLocationMenuIcon()
        if (!previouslyHadLocationPermissions && requirementsChecker.isLocationPermissionCheckPassed()) {
            previouslyHadLocationPermissions = true
            mapFragment.myLocationEnabled()
            service?.reInitializeLocationRequests()
        }
        viewModel?.refreshMarkers()
    }

    private fun handleIntentExtras(intent: Intent) {
        Timber.v("handleIntentExtras")
        val b = navigator.getExtrasBundle(intent)
        if (b != null) {
            Timber.v("intent has extras from drawerProvider")
            val contactId = b.getString(BUNDLE_KEY_CONTACT_ID)
            if (contactId != null) {
                viewModel!!.setLiveContact(contactId)
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
        updateMyLocationMenuIcon()
        return true
    }

    private fun updateMyLocationMenuIcon() {
        menu?.findItem(R.id.menu_mylocation)?.setIcon(
            if (requirementsChecker.isLocationPermissionCheckPassed() && requirementsChecker.isLocationServiceEnabled()) {
                R.drawable.ic_baseline_my_location_24
            } else {
                R.drawable.ic_baseline_location_disabled_24
            }
        )
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
        return when (item.itemId) {
            R.id.menu_report -> {
                viewModel?.sendLocation()
                true
            }
            R.id.menu_mylocation -> {
                if (checkAndRequestLocationPermissions(true)) {
                    checkAndRequestLocationServicesEnabled(true)
                }
                viewModel?.onMenuCenterDeviceClicked()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_monitoring -> {
                stepMonitoringModeMenu()
                true
            }
            else -> false
        }
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
                Toast.makeText(this, R.string.monitoring_significant, Toast.LENGTH_SHORT)
                    .show()
            }
            else -> {
                Toast.makeText(this, R.string.monitoring_move, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disableLocationMenus() {
        menu?.run {
            findItem(R.id.menu_mylocation).setEnabled(false).icon.alpha = 128
            findItem(R.id.menu_report).setEnabled(false).icon.alpha = 128
        }
    }

    private fun enableLocationMenus() {
        menu?.run {
            findItem(R.id.menu_mylocation).setEnabled(true).icon.alpha = 255
            findItem(R.id.menu_report).setEnabled(true).icon.alpha = 255
        }
    }

    override fun clearMarkers() {
        mapFragment.clearMarkers()
    }

    override fun removeMarker(contact: FusedContact) {
        mapFragment.removeMarker(contact.id)
    }

    override fun updateMarker(contact: FusedContact) {
        if (contact.latLng == null) {
            Timber.w("unable to update marker for $contact. no location")
            return
        }
        Timber.v("updating marker for contact: %s", contact.id)
        mapFragment.updateMarker(contact.id, contact.latLng!!)
        GlobalScope.launch(Dispatchers.Main) {
            contactImageBindingAdapter.run {
                mapFragment.setMarkerImage(contact.id, getBitmapFromCache(contact))
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
        return when (item.itemId) {
            R.id.menu_clear -> {
                viewModel?.onClearContactClicked()
                false
            }
            R.id.menu_navigate -> {
                val c = viewModel!!.contact
                c.value?.latLng?.run {
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("google.navigation:q=${latitude},${longitude}")
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Snackbar.make(
                            binding!!.coordinatorLayout,
                            getString(R.string.noNavigationApp),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                } ?: run {
                    Snackbar.make(
                        binding!!.coordinatorLayout,
                        getString(R.string.contactLocationUnknown),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                true
            }
            else -> false
        }
    }

    override fun onLongClick(view: View): Boolean {
        viewModel!!.onBottomSheetLongClick()
        return true
    }

    override fun setBottomSheetExpanded() {
        bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
        orientationSensor?.let {
            sensorManager?.registerListener(viewModel?.orientationSensorEventListener, it, 500_000)
        }
    }

    // BOTTOM SHEET CALLBACKS
    override fun onClick(view: View) {
        viewModel!!.onBottomSheetClick()
    }

    override fun setBottomSheetCollapsed() {
        bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
        sensorManager?.unregisterListener(viewModel?.orientationSensorEventListener)
    }

    override fun setBottomSheetHidden() {
        bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
        menu?.run { close() }
        sensorManager?.unregisterListener(viewModel?.orientationSensorEventListener)
    }

    private fun showPopupMenu(v: View) {
        val popupMenu = PopupMenu(this, v, Gravity.START)
        popupMenu.menuInflater.inflate(R.menu.menu_popup_contacts, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener(this)
        if (preferences.mode == MessageProcessorEndpointHttp.MODE_ID) {
            popupMenu.menu.removeItem(R.id.menu_clear)
        }
        if (viewModel?.contactHasLocation() == null) {
            popupMenu.menu.removeItem(R.id.menu_navigate)
        }
        popupMenu.show()
    }

    fun onMapReady() {
        viewModel?.onMapReady()
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior == null) {
            super.onBackPressed()

        } else {
            when (bottomSheetBehavior?.state) {
                BottomSheetBehavior.STATE_HIDDEN -> super.onBackPressed()
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    setBottomSheetHidden()
                }
                BottomSheetBehavior.STATE_DRAGGING -> {
                    //Noop
                }
                BottomSheetBehavior.STATE_EXPANDED -> {
                    setBottomSheetCollapsed()
                }
                BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                    setBottomSheetCollapsed()
                }
                BottomSheetBehavior.STATE_SETTLING -> {
                    //Noop
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, BackgroundService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }

    @get:VisibleForTesting
    val locationIdlingResource: IdlingResource?
        get() = binding?.vm?.locationIdlingResource

    @get:VisibleForTesting
    val outgoingQueueIdlingResource: IdlingResource
        get() = countingIdlingResource

    companion object {
        const val BUNDLE_KEY_CONTACT_ID = "BUNDLE_KEY_CONTACT_ID"

        // Paris
        const val STARTING_LATITUDE = 48.856826
        const val STARTING_LONGITUDE = 2.292713

        const val PERMISSIONS_REQUEST_CODE = 1
        const val PERMISSIONS_REQUEST_CODE_WITH_EXPLICIT_SERVICES_CHECK = 2
    }
}