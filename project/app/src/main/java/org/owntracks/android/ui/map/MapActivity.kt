package org.owntracks.android.ui.map

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_UI
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.TooltipCompat
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.core.view.setPadding
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.test.SimpleIdlingResource
import org.owntracks.android.test.ThresholdIdlingResourceInterface
import org.owntracks.android.ui.DrawerProvider
import org.owntracks.android.ui.NotificationsStash
import org.owntracks.android.ui.mixins.BackgroundLocationPermissionRequester
import org.owntracks.android.ui.mixins.LocationPermissionRequester
import org.owntracks.android.ui.mixins.NotificationPermissionRequester
import org.owntracks.android.ui.mixins.ServiceStarter
import org.owntracks.android.ui.mixins.WorkManagerInitExceptionNotifier
import org.owntracks.android.ui.welcome.WelcomeActivity
import timber.log.Timber

@AndroidEntryPoint
class MapActivity :
    AppCompatActivity(),
    View.OnClickListener,
    View.OnLongClickListener,
    WorkManagerInitExceptionNotifier by WorkManagerInitExceptionNotifier.Impl(),
    ServiceStarter by ServiceStarter.Impl() {
  private val viewModel: MapViewModel by viewModels()
  private val notificationPermissionRequester =
      NotificationPermissionRequester(
          this,
          ::notificationPermissionGranted,
          ::notificationPermissionDenied,
      )
  private val locationPermissionRequester =
      LocationPermissionRequester(this, ::locationPermissionGranted, ::locationPermissionDenied)
  private val backgroundLocationPermissionRequester =
      BackgroundLocationPermissionRequester(
          this,
          ::backgroundLocationPermissionGranted,
          ::backgroundLocationPermissionDenied,
      )
  private var service: BackgroundService? = null
  private var bottomSheetBehavior: BottomSheetBehavior<LinearLayoutCompat>? = null
  private var menu: Menu? = null
  private var sensorManager: SensorManager? = null
  private var orientationSensor: Sensor? = null
  private lateinit var binding: UiMapBinding

  private lateinit var backPressedCallback: OnBackPressedCallback

  @Inject lateinit var notificationsStash: NotificationsStash

  @Inject
  @Named("outgoingQueueIdlingResource")
  @get:VisibleForTesting
  lateinit var outgoingQueueIdlingResource: ThresholdIdlingResourceInterface

  @Inject
  @Named("publishResponseMessageIdlingResource")
  @get:VisibleForTesting
  lateinit var publishResponseMessageIdlingResource: SimpleIdlingResource

  @Inject
  @Named("importConfigurationIdlingResource")
  @get:VisibleForTesting
  lateinit var importConfigurationIdlingResource: SimpleIdlingResource

  @Inject lateinit var requirementsChecker: RequirementsChecker

  @Inject lateinit var preferences: Preferences

  @Inject lateinit var drawerProvider: DrawerProvider

  private val serviceConnection =
      object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
          Timber.d("MapActivity connected to service")
          this@MapActivity.service = (service as BackgroundService.LocalBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
          Timber.d("Service disconnected from MapActivity")
          service = null
        }
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    EntryPointAccessors.fromActivity(this, MapActivityEntryPoint::class.java).let {
      supportFragmentManager.fragmentFactory = it.fragmentFactory
    }

    super.onCreate(savedInstanceState)

    if (!preferences.setupCompleted) {
      startActivity(Intent(this, WelcomeActivity::class.java))
      finish()
      return
    }

    binding =
        UiMapBinding.inflate(layoutInflater).apply {
          setContentView(root)
          appbar.toolbar.run {
            setSupportActionBar(this)
            drawerProvider.attach(this)
          }
          bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
          contactPeek.contactRow.setOnClickListener(this@MapActivity)
          contactPeek.contactRow.setOnLongClickListener(this@MapActivity)
          contactClearButton.setOnClickListener { viewModel.onClearContactClicked() }
          requestLocationReportButton.setOnClickListener {
            viewModel.sendLocationRequestToCurrentContact()
          }
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
                                this.displayName,
                                this.geocodedLocation,
                                this.latLng?.toDisplayString() ?: "",
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                    .withZone(ZoneId.systemDefault())
                                    .format(Instant.ofEpochSecond(this.locationTimestamp)),
                            )
                          } ?: R.string.na,
                      )
                    },
                    "Share Location",
                ),
            )
          }
          contactNavigateButton.setOnClickListener { navigateToCurrentContact() }

          // Set behaviour of the mylocation fab
          fabMyLocation.apply {
            TooltipCompat.setTooltipText(this, getString(R.string.currentLocationButtonLabel))
            setOnClickListener {
              if (checkAndRequestLocationPermissions(true) ==
                  CheckPermissionsResult.HAS_PERMISSIONS) {
                // Trigger a check for location services to be enabled
                checkAndRequestLocationServicesEnabled(true)
              }
              if (viewModel.myLocationStatus.value != MyLocationStatus.DISABLED) {
                viewModel.onMyLocationClicked()
              }
            }
          }

          // Set behaviour of the mapLayers fab
          fabMapLayers.apply {
            TooltipCompat.setTooltipText(this, getString(R.string.mapLayerDialogTitle))
            setOnClickListener {
              MapLayerBottomSheetDialog().show(supportFragmentManager, "layerBottomSheetDialog")
            }
          }
          // Need to set the appbar layout behaviour to be non-drag, so that we can drag the map
          (appbar.root.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior =
              AppBarLayout.Behavior().apply {
                setDragCallback(
                    object : AppBarLayout.Behavior.DragCallback() {
                      override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                        return false
                      }
                    },
                )
              }
        }

    supportActionBar?.setDisplayShowTitleEnabled(false)

    val labels =
        listOf(
                binding.contactDetailsAccuracy.root,
                binding.contactDetailsAltitude.root,
                binding.contactDetailsBattery.root,
                binding.contactDetailsBearing.root,
                binding.contactDetailsSpeed.root,
                binding.contactDetailsDistance.root,
            )
            .map { it.findViewById<AutoResizingTextViewWithListener>(R.id.label) }

    object : AutoResizingTextViewWithListener.OnTextSizeChangedListener {
          @SuppressLint("RestrictedApi")
          override fun onTextSizeChanged(view: View, newSize: Float) {
            labels
                .filter { it != view }
                .filter { it.textSize > newSize || it.configurationChangedFlag }
                .forEach {
                  it.setAutoSizeTextTypeUniformWithPresetSizes(
                      intArrayOf(newSize.toInt()),
                      TypedValue.COMPLEX_UNIT_PX,
                  )
                  it.configurationChangedFlag = false
                }
          }
        }
        .also { listener -> labels.forEach { it.withListener(listener) } }
    backPressedCallback =
        onBackPressedDispatcher.addCallback(this, false) {
          when (bottomSheetBehavior?.state) {
            BottomSheetBehavior.STATE_COLLAPSED -> {
              setBottomSheetHidden()
            }

            BottomSheetBehavior.STATE_EXPANDED -> {
              setBottomSheetCollapsed()
            }

            else -> {
              // If the bottom sheet is hidden, we want to finish the activity
              finish()
            }
          }
        }

    setBottomSheetHidden()

    /* Observe some things, react to them! */

    viewModel.apply {
      lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
          launch {
            locationRequestContactCommandFlow.collect { contact ->
              Snackbar.make(
                      binding.root, getString(R.string.requestLocationSent), Snackbar.LENGTH_SHORT)
                  .show()
            }
          }
        }
      }

      currentContact.observe(this@MapActivity) { contact: Contact? ->
        contact?.let {
          binding.contactPeek.run {
            image.setImageResource(0) // Remove old image before async loading the new one
            lifecycleScope.launch {
              //            contactImageBindingAdapter.run {
              // image.setImageBitmap(getBitmapFromCache(it)) } // TODO fix
            }
            binding.apply {
              contactDetails.visibility = if (it.latLng != null) View.VISIBLE else View.GONE
              contactDetailsAccuracy.value.text =
                  getString(R.string.contactDetailsAccuracyValue, it.locationAccuracy)
              contactDetailsAltitude.value.text =
                  getString(R.string.contactDetailsAltitudeValue, it.altitude)
              contactDetailsBattery.value.text =
                  it.battery?.let { b -> getString(R.string.contactDetailsBatteryValue, b) }
                      ?: getString(R.string.na)
              contactDetailsSpeed.value.text =
                  getString(R.string.contactDetailsSpeedValue, it.velocity)
              contactDetailsTrackerId.value.text = it.trackerId
              contactDetailsTopic.value.text = it.id
              contactDetailsCoordinates.value.text = it.latLng?.toDisplayString()
              contactDetailsCoordinates.root.visibility =
                  if (it.latLng != null) View.VISIBLE else View.GONE
              contactNavigateButton.visibility = if (it.latLng != null) View.VISIBLE else View.GONE
              contactShareButton.visibility = if (it.latLng != null) View.VISIBLE else View.GONE
            }

            //          contactName.text = it.displayName
            //          contactLastseen.text =
            //              DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            //                  .withZone(ZoneId.systemDefault())
            //                  .format(Instant.ofEpochSecond(it.locationTimestamp))
          }
        }
      }
      bottomSheetHidden.observe(this@MapActivity) { o: Boolean? ->
        if (o == null || o) {
          setBottomSheetHidden()
        } else {
          setBottomSheetCollapsed()
        }
      }
      currentLocation.observe(this@MapActivity) { location ->
        if (location == null) {
          disableLocationMenus()
        } else {
          enableLocationMenus()
          viewModel.updateActiveContactDistanceAndBearing(location)
        }
        currentMonitoringMode.observe(this@MapActivity) { updateMonitoringModeMenu() }
      }
      myLocationStatus.observe(this@MapActivity) { status ->
        binding.fabMyLocation.run {
          val tint =
              when (status) {
                MyLocationStatus.FOLLOWING ->
                    resources.getColor(R.color.fabMyLocationForegroundActiveTint, null)

                else -> resources.getColor(R.color.fabMyLocationForegroundInActiveTint, null)
              }
          ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(tint))
          when (status) {
            MyLocationStatus.DISABLED ->
                setImageResource(R.drawable.ic_baseline_location_disabled_24)
            MyLocationStatus.AVAILABLE ->
                setImageResource(R.drawable.ic_baseline_location_searching_24)

            MyLocationStatus.FOLLOWING -> setImageResource(R.drawable.ic_baseline_my_location_24)
            else -> setImageResource(R.drawable.ic_baseline_location_disabled_24)
          }
        }
      }

      contactDistance.observe(this@MapActivity) { distance ->
        binding.contactDetailsBearing.value.text =
            getString(R.string.contactDetailsBearingValue, distance)
      }
      relativeContactBearing.observe(this@MapActivity) { relativeBearing ->
        binding.contactDetailsBearing.icon.rotation = relativeBearing
      }
      contactBearing.observe(this@MapActivity) { bearing ->
        binding.distanceBearingDetails.visibility =
            if (bearing != null) {
              binding.contactDetailsDistance.value.text =
                  getString(
                      R.string.contactDetailsDistanceValue,
                      if (bearing > 1000f) bearing / 1000 else bearing,
                      if (bearing > 1000f) getString(R.string.contactDetailsDistanceUnitKilometres)
                      else getString(R.string.contactDetailsDistanceUnitMeters),
                  )

              View.VISIBLE
            } else {
              View.GONE
            }
      }
    }
    startService(this)

    // We've been started in the foreground, so cancel the background restriction notification
    NotificationManagerCompat.from(this).cancel(BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG, 0)

    notifyOnWorkManagerInitFailure(this)
  }

  private fun navigateToCurrentContact() {
    viewModel.currentContact.value?.latLng?.apply {
      try {
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                "google.navigation:q=${latitude.value.roundForDisplay()},${longitude.value.roundForDisplay()}"
                    .toUri(),
            )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
      } catch (_: ActivityNotFoundException) {
        Snackbar.make(
                binding.mapCoordinatorLayout,
                getString(R.string.noNavigationApp),
                Snackbar.LENGTH_SHORT,
            )
            .show()
      }
    }
        ?: run {
          Snackbar.make(
                  binding.mapCoordinatorLayout,
                  getString(R.string.contactLocationUnknown),
                  Snackbar.LENGTH_SHORT,
              )
              .show()
        }
  }

  private val locationServicesLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // We have to check permissions again here, because it may have been revoked in the
        // period between asking for location services and returning here.
        Timber.d("Location services callback, result=$it")
        if (checkAndRequestLocationPermissions(false) == CheckPermissionsResult.HAS_PERMISSIONS) {
          viewModel.requestLocationUpdatesForBlueDot()
        }
      }

  /**
   * Performs a check that the device has location services enabled. This can be called either
   * because the user has explicitly done something that requires the location services (clicked on
   * the MyLocation FAB), or because some other action has happened and we need to re-check that the
   * location service is enabled (e.g. onResume, or location has become unavailable).
   *
   * This check may trigger a request and prompt the user to enable location services. This prompt
   * should be raised either if it's an explicit request, or if the user hasn't previously declined
   * to enable location services.
   *
   * @param explicitUserAction Indicates whether or not the user has triggered something explicitly
   *   causing a location services check
   * @return indication as to whether location services are enabled. This will return false
   *   immediately if a prompt to enable to raised, even if the user says "yes" to the prompt.
   */
  private fun checkAndRequestLocationServicesEnabled(explicitUserAction: Boolean): Boolean {
    Timber.d("Check and request location services")
    return if (!requirementsChecker.isLocationServiceEnabled()) {
      Timber.d("Location Services disabled")
      if ((explicitUserAction || !preferences.userDeclinedEnableLocationServices)) {
        Timber.d("Showing location services dialog")
        MaterialAlertDialogBuilder(this)
            .setCancelable(true)
            .setIcon(R.drawable.ic_baseline_location_disabled_24)
            .setTitle(getString(R.string.deviceLocationDisabledDialogTitle))
            .setMessage(getString(R.string.deviceLocationDisabledDialogMessage))
            .setPositiveButton(
                getString(R.string.deviceLocationDisabledDialogPositiveButtonLabel),
            ) { _, _ ->
              locationServicesLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
              preferences.userDeclinedEnableLocationServices = true
            }
            .show()
      } else {
        Timber.d(
            "Not requesting location services. " +
                "Explicit=false, previously declined=${preferences.userDeclinedEnableLocationServices}",
        )
      }
      false
    } else {
      Timber.d("Location services enabled")
      true
    }
  }

  /** User has granted notification permission. Notify everything that's in the NotificationStash */
  @RequiresPermission(POST_NOTIFICATIONS)
  private fun notificationPermissionGranted() {
    Timber.d("Notification permission granted. Showing all notifications in stash")
    notificationsStash.showAll(NotificationManagerCompat.from(this))
  }

  /**
   * User has declined notification permissions. Log this in the preferences so we don't keep asking
   * them
   */
  private fun notificationPermissionDenied() {
    Timber.d("Notification permission denied")
    preferences.userDeclinedEnableNotificationPermissions = true
  }

  /**
   * User has granted permission. If It's location, ask the viewmodel to start requesting locations,
   * set the [MapViewModel.ViewMode] to [MapViewModel.ViewMode.Device] and tell the service to
   * reinitialize locations.
   */
  private fun locationPermissionGranted(code: Int) {
    Timber.d("Location Permission granted")
    if (code == EXPLICIT_LOCATION_PERMISSION_REQUEST) {
      Timber.d("Location Permission was explicitly asked for, check location services")
      checkAndRequestLocationServicesEnabled(true)
    }
    viewModel.requestLocationUpdatesForBlueDot()
    viewModel.onMyLocationClicked()
    viewModel.updateMyLocationStatus()
    service?.reInitializeLocationRequests()
  }

  /**
   * User has declined to enable location permissions. [Snackbar] the user with the option of trying
   * again (in case they didn't mean to).
   */
  private fun locationPermissionDenied(@Suppress("UNUSED_PARAMETER") code: Int) {
    Timber.d("Location Permission denied. Showing snackbar")
    preferences.userDeclinedEnableLocationPermissions = true
    Snackbar.make(
            binding.mapCoordinatorLayout,
            getString(R.string.locationPermissionNotGrantedNotification),
            Snackbar.LENGTH_LONG,
        )
        .setAction(getString(R.string.fixProblemLabel)) {
          startActivity(
              Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
              },
          )
        }
        .show()
  }

  /**
   * User has declined to enable background location permissions. Log this in the preferences so we
   * don't keep asking
   */
  private fun backgroundLocationPermissionGranted() {
    Timber.d("Background location permission granted")
    preferences.userDeclinedEnableBackgroundLocationPermissions = false
  }

  /**
   * User has declined to enable background location permissions. Log this in the preferences so we
   * don't keep asking.
   *
   * This may have been called by the user calling cancel on the material dialog in the onResume
   * flow so we need to check for location services next
   */
  private fun backgroundLocationPermissionDenied() {
    Timber.d("Background location permission denied")
    preferences.userDeclinedEnableBackgroundLocationPermissions = true
    if (checkAndRequestLocationServicesEnabled(false)) {
      viewModel.requestLocationUpdatesForBlueDot()
    }
  }

  enum class CheckPermissionsResult {
    HAS_PERMISSIONS,
    NO_PERMISSIONS_LAUNCHED_REQUEST,
    NO_PERMISSIONS_NOT_LAUNCHED_REQUEST
  }

  private fun checkAndRequestNotificationPermissions(): CheckPermissionsResult {
    Timber.d("Checking and requesting notification permissions")
    return if (!notificationPermissionRequester.hasPermission()) {
      Timber.d("No notification permission")
      if (!preferences.userDeclinedEnableNotificationPermissions) {
        Timber.d("Requesting notification permissions")
        notificationPermissionRequester.requestNotificationPermission()
        CheckPermissionsResult.NO_PERMISSIONS_LAUNCHED_REQUEST
      } else {
        Timber.d(
            "Not request location permissions. " +
                "previouslyDeclined=${preferences.userDeclinedEnableNotificationPermissions}",
        )
        CheckPermissionsResult.NO_PERMISSIONS_NOT_LAUNCHED_REQUEST
      }
    } else {
      CheckPermissionsResult.HAS_PERMISSIONS
    }
  }

  /**
   * Performs a check that the user has granted location permissions. This can be called either
   * because the user has explicitly done something that requires location permissions (clicked on
   * the MyLocation FAB), or because some other action has happened and we need to re-check location
   * permissions (e.g. user has enabled location services).
   *
   * This check may trigger a request and prompt the user to grant location permissions. This prompt
   * should be raised either if it's an explicit request for something that needs permissions, or if
   * the user hasn't previously denied location permission.
   *
   * @param explicitUserAction Indicates whether or not the user has triggered something explicitly
   *   causing a permissions check
   * @return indication as to whether location permissions have been granted. This will return false
   *   immediately if a prompt to enable to raised, even if the user says "yes" to the prompt.
   */
  private fun checkAndRequestLocationPermissions(
      explicitUserAction: Boolean
  ): CheckPermissionsResult {
    Timber.d("Checking and requesting location permissions")
    return if (!requirementsChecker.hasLocationPermissions()) {
      Timber.d("No location permission")
      // We don't have location permission
      if ((explicitUserAction || !preferences.userDeclinedEnableLocationPermissions)) {
        Timber.d("Requesting location permissions, explicit=$explicitUserAction")
        locationPermissionRequester.requestLocationPermissions(
            if (explicitUserAction) {
              EXPLICIT_LOCATION_PERMISSION_REQUEST
            } else {
              IMPLICIT_LOCATION_PERMISSION_REQUEST
            },
            this,
        ) {
          shouldShowRequestPermissionRationale(it)
        }
        CheckPermissionsResult.NO_PERMISSIONS_LAUNCHED_REQUEST
      } else {
        Timber.d(
            "Not request location permissions. " +
                "Explicit action=false, previouslyDeclined=${preferences.userDeclinedEnableLocationPermissions}",
        )
        CheckPermissionsResult.NO_PERMISSIONS_NOT_LAUNCHED_REQUEST
      }
    } else {
      CheckPermissionsResult.HAS_PERMISSIONS
    }
  }

  private fun checkAndRequestBackgroundLocationPermissions(): CheckPermissionsResult {
    Timber.d("Checking and requesting background location permissions")
    return if (!requirementsChecker.hasBackgroundLocationPermission()) {
      Timber.d("No background location permission")
      if (!preferences.userDeclinedEnableBackgroundLocationPermissions &&
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Timber.d("Requesting background location permissions")
        backgroundLocationPermissionRequester.requestLocationPermissions(this) { true }
        CheckPermissionsResult.NO_PERMISSIONS_LAUNCHED_REQUEST
      } else {
        Timber.d("Not requesting background location permission")
        CheckPermissionsResult.NO_PERMISSIONS_NOT_LAUNCHED_REQUEST
      }
    } else {
      preferences.userDeclinedEnableBackgroundLocationPermissions = true
      CheckPermissionsResult.HAS_PERMISSIONS
    }
  }

  override fun onResume() {
    val mapFragment =
        supportFragmentManager.fragmentFactory.instantiate(
            this.classLoader,
            MapFragment::class.java.name,
        )
    supportFragmentManager.commit(true) { replace(R.id.mapFragment, mapFragment, "map") }
    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    sensorManager?.let {
      orientationSensor = it.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
      orientationSensor?.run { Timber.d("Got a rotation vector sensor") }
    }
    super.onResume()
    updateMonitoringModeMenu()
    viewModel.updateMyLocationStatus()

    if (checkAndRequestNotificationPermissions() ==
        CheckPermissionsResult.NO_PERMISSIONS_LAUNCHED_REQUEST) {
      Timber.d("Launched notification permission request")
      return
    }
    if (checkAndRequestLocationPermissions(false) ==
        CheckPermissionsResult.NO_PERMISSIONS_LAUNCHED_REQUEST) {
      Timber.d("Launched location permission request")
      return
    }
    if (checkAndRequestBackgroundLocationPermissions() ==
        CheckPermissionsResult.NO_PERMISSIONS_LAUNCHED_REQUEST) {
      Timber.d("Launched background location permission request")
      return
    }
    if (checkAndRequestLocationServicesEnabled(false)) {
      viewModel.requestLocationUpdatesForBlueDot()
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
    menu?.findItem(R.id.menu_monitoring)?.run {
      when (preferences.monitoring) {
        MonitoringMode.Quiet -> {
          setIcon(R.drawable.ic_baseline_stop_36)
          setTitle(R.string.monitoring_quiet)
        }

        MonitoringMode.Manual -> {
          setIcon(R.drawable.ic_baseline_pause_36)
          setTitle(R.string.monitoring_manual)
        }

        MonitoringMode.Significant -> {
          setIcon(R.drawable.ic_baseline_play_arrow_36)
          setTitle(R.string.monitoring_significant)
        }

        MonitoringMode.Move -> {
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
        MonitoringModeBottomSheetDialog().show(supportFragmentManager, "modeBottomSheetDialog")
        true
      }

      else -> false
    }
  }

  private fun disableLocationMenus() {
    binding.fabMyLocation.isEnabled = false
    menu?.run { findItem(R.id.menu_report).setEnabled(false).icon?.alpha = 128 }
  }

  private fun enableLocationMenus() {
    binding.fabMyLocation.isEnabled = true
    menu?.run { findItem(R.id.menu_report).setEnabled(true).icon?.alpha = 255 }
  }

  override fun onLongClick(view: View): Boolean {
    viewModel.onBottomSheetLongClick()
    return true
  }

  private fun setBottomSheetExpanded() {
    bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
    binding.mapFragment.setPaddingRelative(0, 0, 0, binding.bottomSheetLayout.height)
    orientationSensor?.let {
      sensorManager?.registerListener(viewModel.orientationSensorEventListener, it, SENSOR_DELAY_UI)
    }
    backPressedCallback.isEnabled = true
  }

  // BOTTOM SHEET CALLBACKS
  override fun onClick(view: View) {
    setBottomSheetExpanded()
  }

  private fun setBottomSheetCollapsed() {
    bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
    binding.mapFragment.setPadding(0)
    sensorManager?.unregisterListener(viewModel.orientationSensorEventListener)
    backPressedCallback.isEnabled = true
  }

  private fun setBottomSheetHidden() {
    bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
    binding.mapFragment.setPadding(0)
    menu?.run { close() }
    sensorManager?.unregisterListener(viewModel.orientationSensorEventListener)
    backPressedCallback.isEnabled = false
  }

  override fun onStart() {
    super.onStart()
    bindService(
        Intent(this, BackgroundService::class.java),
        serviceConnection,
        Context.BIND_AUTO_CREATE,
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
