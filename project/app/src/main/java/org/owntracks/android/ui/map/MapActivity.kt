package org.owntracks.android.ui.map

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.TooltipCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.setPadding
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.test.espresso.idling.CountingIdlingResource
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.databinding.UiMapBinding
import org.owntracks.android.location.roundForDisplay
import org.owntracks.android.model.Contact
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.services.BackgroundService.Companion.BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG
import org.owntracks.android.support.ContactImageBindingAdapter
import org.owntracks.android.support.DrawerProvider
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.support.SimpleIdlingResource
import org.owntracks.android.ui.mixins.ActivityResultCallerWithLocationPermissionCallback
import org.owntracks.android.ui.mixins.LocationPermissionRequester
import org.owntracks.android.ui.mixins.NotificationsPermissionRequested
import org.owntracks.android.ui.mixins.ServiceStarter
import org.owntracks.android.ui.mixins.WorkManagerInitExceptionNotifier
import org.owntracks.android.ui.welcome.WelcomeActivity
import org.threeten.bp.ZoneId
import timber.log.Timber

@AndroidEntryPoint
class MapActivity :
    AppCompatActivity(),
    View.OnClickListener,
    View.OnLongClickListener,
    ActivityResultCallerWithLocationPermissionCallback,
    WorkManagerInitExceptionNotifier by WorkManagerInitExceptionNotifier.Impl(),
    ServiceStarter by ServiceStarter.Impl(),
    NotificationsPermissionRequested by NotificationsPermissionRequested.Impl() {
    private val viewModel: MapViewModel by viewModels()
    private val locationPermissionRequester = LocationPermissionRequester(this)
    private var previouslyHadLocationPermissions: Boolean = false
    private var service: BackgroundService? = null
    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayoutCompat>? = null
    private var menu: Menu? = null
    private var sensorManager: SensorManager? = null
    private var orientationSensor: Sensor? = null
    private lateinit var binding: UiMapBinding

    private lateinit var locationServicesAlertDialog: AlertDialog

    @Inject
    lateinit var contactImageBindingAdapter: ContactImageBindingAdapter

    @Inject
    @Named("outgoingQueueIdlingResource")
    @get:VisibleForTesting
    lateinit var outgoingQueueIdlingResource: CountingIdlingResource

    @Inject
    @Named("publishResponseMessageIdlingResource")
    @get:VisibleForTesting
    lateinit var publishResponseMessageIdlingResource: SimpleIdlingResource

    @Inject
    @Named("importConfigurationIdlingResource")
    @get:VisibleForTesting
    lateinit var importConfigurationIdlingResource: SimpleIdlingResource

    @Inject
    lateinit var requirementsChecker: RequirementsChecker

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var drawerProvider: DrawerProvider

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
        EntryPointAccessors.fromActivity(
            this,
            MapActivityEntryPoint::class.java
        )
            .let {
                supportFragmentManager.fragmentFactory = it.fragmentFactory
            }

        super.onCreate(savedInstanceState)

        if (!preferences.setupCompleted) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }
        postNotificationsPermissionInit(this, preferences)

        binding = DataBindingUtil.setContentView<UiMapBinding>(this, R.layout.ui_map)
            .apply {
                vm = viewModel
                lifecycleOwner = this@MapActivity
                appbar.toolbar.run {
                    setSupportActionBar(this)
                    drawerProvider.attach(this)
                }
                supportActionBar?.setDisplayShowTitleEnabled(false)
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
                contactPeek.contactRow.setOnClickListener(this@MapActivity)
                contactPeek.contactRow.setOnLongClickListener(this@MapActivity)
                contactClearButton.setOnClickListener { viewModel.onClearContactClicked() }
                contactShareButton.setOnClickListener {
                    startActivity(
                        Intent.createChooser(
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    viewModel.currentContact.value?.run {
                                        getString(
                                            R.string.shareContactBody,
                                            this.name,
                                            this.geocodedLocation,
                                            this.latLng?.toDisplayString() ?: "",
                                            org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                                .withZone(ZoneId.systemDefault()).format(
                                                    org.threeten.bp.Instant.ofEpochSecond(this.tst)
                                                )

                                        )
                                    } ?: R.string.na
                                )
                            },
                            "Share Location"
                        )
                    )
                }

                contactNavigateButton.setOnClickListener { navigateToCurrentContact() }

                // Need to set the appbar layout behaviour to be non-drag, so that we can drag the map
                AppBarLayout.Behavior()
                    .setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                        override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                            return false
                        }
                    })

                fabMyLocation.apply {
                    TooltipCompat.setTooltipText(this, getString(R.string.currentLocationButtonLabel))
                    setOnClickListener {
                        if (checkAndRequestLocationPermissions(true)) {
                            checkAndRequestLocationServicesEnabled(true)
                        }
                        if (viewModel.myLocationStatus.value != MyLocationStatus.DISABLED) {
                            viewModel.onMyLocationClicked()
                        }
                    }
                }

                fabMapLayers.apply {
                    TooltipCompat.setTooltipText(this, getString(R.string.mapLayerDialogTitle))
                    setOnClickListener {
                        MapLayerBottomSheetDialog().show(
                            supportFragmentManager,
                            "layerBottomSheetDialog"
                        )
                    }
                }

                val labels = listOf(
                    R.id.contactDetailsAccuracy,
                    R.id.contactDetailsAltitude,
                    R.id.contactDetailsBattery,
                    R.id.contactDetailsBearing,
                    R.id.contactDetailsSpeed,
                    R.id.contactDetailsDistance
                ).map { bottomSheetLayout.findViewById<View>(it) }
                    .map { it.findViewById<AutoResizingTextViewWithListener>(R.id.label) }

                object : AutoResizingTextViewWithListener.OnTextSizeChangedListener {
                    @SuppressLint("RestrictedApi")
                    override fun onTextSizeChanged(view: View, newSize: Float) {
                        labels.filter { it != view }
                            .filter { it.textSize > newSize || it.configurationChangedFlag }
                            .forEach {
                                it.setAutoSizeTextTypeUniformWithPresetSizes(
                                    intArrayOf(newSize.toInt()),
                                    TypedValue.COMPLEX_UNIT_PX
                                )
                                it.configurationChangedFlag = false
                            }
                    }
                }.also { listener -> labels.forEach { it.withListener(listener) } }
            }

        setBottomSheetHidden()

        viewModel.currentContact.observe(this) { contact: Contact? ->
            contact?.let {
                binding.contactPeek.run {
                    image.setImageResource(0) // Remove old image before async loading the new one
                    lifecycleScope.launch {
                        contactImageBindingAdapter.run {
                            image.setImageBitmap(
                                getBitmapFromCache(it)
                            )
                        }
                    }
                }
            }
        }
        viewModel.bottomSheetHidden.observe(this) { o: Boolean? ->
            if (o == null || o) {
                setBottomSheetHidden()
            } else {
                setBottomSheetCollapsed()
            }
        }
        viewModel.currentLocation.observe(this) { location ->
            if (location == null) {
                disableLocationMenus()
            } else {
                enableLocationMenus()
                binding.vm?.run {
                    updateActiveContactDistanceAndBearing(location)
                }
            }
        }
        viewModel.currentMonitoringMode.observe(this) {
            updateMonitoringModeMenu()
        }

        startService(this)

        // We've been started in the foreground, so cancel the background restriction notification
        NotificationManagerCompat.from(this)
            .cancel(BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG, 0)

        notifyOnWorkManagerInitFailure(this)

        onBackPressedDispatcher.addCallback(this) {
            if (bottomSheetBehavior == null) {
                finish()
            } else {
                when (bottomSheetBehavior?.state) {
                    BottomSheetBehavior.STATE_HIDDEN -> finish()
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        setBottomSheetHidden()
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {
                        // Noop
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        setBottomSheetCollapsed()
                    }
                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                        setBottomSheetCollapsed()
                    }
                    BottomSheetBehavior.STATE_SETTLING -> {
                        // Noop
                    }
                }
            }
        }
    }

    private fun navigateToCurrentContact() {
        viewModel.currentContact.value?.latLng?.apply {
            try {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=${latitude.roundForDisplay()},${longitude.roundForDisplay()}")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Snackbar.make(
                    binding.mapCoordinatorLayout,
                    getString(R.string.noNavigationApp),
                    Snackbar.LENGTH_SHORT
                )
                    .show()
            }
        } ?: run {
            Snackbar.make(
                binding.mapCoordinatorLayout,
                getString(R.string.contactLocationUnknown),
                Snackbar.LENGTH_SHORT
            )
                .show()
        }
    }

    private val locationServicesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // We have to check permissions again here, because it may have been revoked in the
        // period between asking for location services and returning here.
        if (checkAndRequestLocationPermissions(false)) {
            viewModel.requestLocationUpdatesForBlueDot()
        }
    }

    /**
     * Performs a check that the device has location services enabled. This can be called either because the user has
     * explicitly done something that requires the location services (clicked on the MyLocation FAB), or because some
     * other action has happened and we need to re-check that the location service is enabled (e.g. onResume, or location
     * has become unavailable).
     *
     * This check may trigger a request and prompt the user to enable location services. This prompt should be raised
     * either if it's an explicit request, or if the user hasn't previously declined to enable location services.
     *
     * @param explicitUserAction Indicates whether or not the user has triggered something explicitly causing a location services check
     * @return indication as to whether location services are enabled. This will return false immediately if a
     * prompt to enable to raised, even if the user says "yes" to the prompt.
     */
    private fun checkAndRequestLocationServicesEnabled(explicitUserAction: Boolean): Boolean {
        return if (!requirementsChecker.isLocationServiceEnabled()) {
            Timber.d("Location Services disabled")
            if ((explicitUserAction || !preferences.userDeclinedEnableLocationServices)) {
                if (!this::locationServicesAlertDialog.isInitialized) {
                    locationServicesAlertDialog = MaterialAlertDialogBuilder(this).setCancelable(true)
                        .setIcon(R.drawable.ic_baseline_location_disabled_24)
                        .setTitle(getString(R.string.deviceLocationDisabledDialogTitle))
                        .setMessage(getString(R.string.deviceLocationDisabledDialogMessage))
                        .setPositiveButton(
                            getString(R.string.deviceLocationDisabledDialogPositiveButtonLabel)
                        ) { _, _ ->
                            locationServicesLauncher.launch(
                                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            )
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            preferences.userDeclinedEnableLocationServices = true
                        }
                        .create()
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

    /**
     * User has granted location permission. Ask the viewmodel to start requesting locations, set the [MapViewModel.ViewMode] to
     * [MapViewModel.ViewMode.Device] and tell the service to reinitialize locations.
     *
     */
    override fun locationPermissionGranted(code: Int) {
        if (code == EXPLICIT_LOCATION_PERMISSION_REQUEST) {
            checkAndRequestLocationServicesEnabled(true)
        }
        viewModel.requestLocationUpdatesForBlueDot()
        viewModel.onMyLocationClicked()
        viewModel.updateMyLocationStatus()
        service?.reInitializeLocationRequests()
    }

    /**
     * User has declined to enable location permissions. [Snackbar] the user with the option of trying again (in case they
     * didn't mean to).
     *
     */
    override fun locationPermissionDenied(code: Int) {
        preferences.userDeclinedEnableLocationPermissions = true
        Snackbar.make(
            binding.mapCoordinatorLayout,
            getString(R.string.locationPermissionNotGrantedNotification),
            Snackbar.LENGTH_LONG
        )
            .setAction(getString(R.string.fixProblemLabel)) {
                startActivity(
                    Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            }
            .show()
    }

    /**
     * Performs a check that the user has granted location permissions. This can be called either because the user has
     * explicitly done something that requires location permissions (clicked on the MyLocation FAB), or because some other
     * action has happened and we need to re-check location permissions (e.g. user has enabled location services).
     *
     * This check may trigger a request and prompt the user to grant location permissions. This prompt should be raised
     * either if it's an explicit request for something that needs permissions, or if the user hasn't previously denied
     * location permission.
     *
     * @param explicitUserAction Indicates whether or not the user has triggered something explicitly causing a permissions check
     * @return indication as to whether location permissions have been granted. This will return false immediately if a
     * prompt to enable to raised, even if the user says "yes" to the prompt.
     */
    private fun checkAndRequestLocationPermissions(explicitUserAction: Boolean): Boolean {
        return if (!requirementsChecker.hasLocationPermissions()) {
            Timber.d("No location permission")
            // We don't have location permission
            if ((explicitUserAction || !preferences.userDeclinedEnableLocationPermissions)) {
                locationPermissionRequester.requestLocationPermissions(
                    if (explicitUserAction) {
                        EXPLICIT_LOCATION_PERMISSION_REQUEST
                    } else {
                        IMPLICIT_LOCATION_PERMISSION_REQUEST
                    },
                    this
                ) { shouldShowRequestPermissionRationale(it) }
            }
            false
        } else {
            true
        }
    }

    override fun onResume() {
        val mapFragment = supportFragmentManager.fragmentFactory.instantiate(
            this.classLoader,
            MapFragment::class.java.name
        )
        supportFragmentManager.commit(true) {
            replace(R.id.mapFragment, mapFragment, "map")
        }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.let {
            orientationSensor = it.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            orientationSensor?.run { Timber.d("Got a rotation vector sensor") }
        }
        super.onResume()
        updateMonitoringModeMenu()
        viewModel.updateMyLocationStatus()

        requestNotificationsPermission()

        if (checkAndRequestLocationPermissions(false)) {
            checkAndRequestLocationServicesEnabled(false)
        }
        if (!previouslyHadLocationPermissions && requirementsChecker.hasLocationPermissions()) {
            previouslyHadLocationPermissions = true
            viewModel.requestLocationUpdatesForBlueDot()
            service?.reInitializeLocationRequests()
        }
    }

    private fun handleIntentExtras(intent: Intent) {
        Timber.v("handleIntentExtras")
        val b = if (intent.hasExtra("_args")) intent.getBundleExtra("_args") else Bundle()
        if (b != null) {
            Timber.v("intent has extras from drawerProvider")
            val contactId = b.getString(BUNDLE_KEY_CONTACT_ID)
            if (contactId != null) {
                viewModel.setLiveContact(contactId)
            }
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        service?.clearEventStackNotification()
        handleIntentExtras(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_map, menu)
        this.menu = menu
        updateMonitoringModeMenu()
        viewModel.updateMyLocationStatus()
        return true
    }

    private fun updateMonitoringModeMenu() {
        menu?.findItem(R.id.menu_monitoring)
            ?.run {
                when (preferences.monitoring) {
                    MonitoringMode.QUIET -> {
                        setIcon(R.drawable.ic_baseline_stop_36)
                        setTitle(R.string.monitoring_quiet)
                    }
                    MonitoringMode.MANUAL -> {
                        setIcon(R.drawable.ic_baseline_pause_36)
                        setTitle(R.string.monitoring_manual)
                    }
                    MonitoringMode.SIGNIFICANT -> {
                        setIcon(R.drawable.ic_baseline_play_arrow_36)
                        setTitle(R.string.monitoring_significant)
                    }
                    MonitoringMode.MOVE -> {
                        setIcon(R.drawable.ic_step_forward_2)
                        setTitle(R.string.monitoring_move)
                    }
                }
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_report -> {
                viewModel.sendLocation()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_monitoring -> {
                MonitoringModeBottomSheetDialog().show(
                    supportFragmentManager,
                    "modeBottomSheetDialog"
                )
                true
            }
            else -> false
        }
    }

    private fun disableLocationMenus() {
        binding.fabMyLocation.isEnabled = false
        menu?.run {
            findItem(R.id.menu_report).setEnabled(false).icon?.alpha = 128
        }
    }

    private fun enableLocationMenus() {
        binding.fabMyLocation.isEnabled = true
        menu?.run {
            findItem(R.id.menu_report).setEnabled(true).icon?.alpha = 255
        }
    }

    override fun onLongClick(view: View): Boolean {
        viewModel.onBottomSheetLongClick()
        return true
    }

    private fun setBottomSheetExpanded() {
        bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
        binding.mapFragment.setPaddingRelative(0, 0, 0, binding.bottomSheetLayout.height)
        orientationSensor?.let {
            sensorManager?.registerListener(
                viewModel.orientationSensorEventListener,
                it,
                500_000
            )
        }
    }

    // BOTTOM SHEET CALLBACKS
    override fun onClick(view: View) {
        setBottomSheetExpanded()
    }

    private fun setBottomSheetCollapsed() {
        bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
        binding.mapFragment.setPadding(0)
        sensorManager?.unregisterListener(viewModel.orientationSensorEventListener)
    }

    private fun setBottomSheetHidden() {
        bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
        binding.mapFragment.setPadding(0)
        menu?.run { close() }
        sensorManager?.unregisterListener(viewModel.orientationSensorEventListener)
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

    companion object {
        const val BUNDLE_KEY_CONTACT_ID = "BUNDLE_KEY_CONTACT_ID"
        const val IMPLICIT_LOCATION_PERMISSION_REQUEST = 1
        const val EXPLICIT_LOCATION_PERMISSION_REQUEST = 2
    }
}

@BindingAdapter("locationIcon")
fun FloatingActionButton.setIcon(status: MyLocationStatus) {
    val tint = when (status) {
        MyLocationStatus.FOLLOWING -> resources.getColor(R.color.fabMyLocationForegroundActiveTint, null)
        else -> resources.getColor(R.color.fabMyLocationForegroundInActiveTint, null)
    }
    when (status) {
        MyLocationStatus.DISABLED -> setImageResource(R.drawable.ic_baseline_location_disabled_24)
        MyLocationStatus.AVAILABLE -> setImageResource(R.drawable.ic_baseline_location_searching_24)
        MyLocationStatus.FOLLOWING -> setImageResource(R.drawable.ic_baseline_my_location_24)
    }
    ImageViewCompat.setImageTintList(
        this,
        ColorStateList.valueOf(tint)
    )
}
