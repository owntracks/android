package org.owntracks.android.ui.map

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.location.roundForDisplay
import org.owntracks.android.model.Contact
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.services.BackgroundService.Companion.BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG
import org.owntracks.android.support.ContactImageBindingAdapter
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.test.SimpleIdlingResource
import org.owntracks.android.test.ThresholdIdlingResourceInterface
import org.owntracks.android.ui.NotificationsStash
import org.owntracks.android.ui.common.CustomToastHost
import org.owntracks.android.ui.common.rememberToastState
import org.owntracks.android.ui.mixins.BackgroundLocationPermissionRequester
import org.owntracks.android.ui.mixins.LocationPermissionRequester
import org.owntracks.android.ui.mixins.NotificationPermissionRequester
import org.owntracks.android.ui.mixins.ServiceStarter
import org.owntracks.android.ui.mixins.WorkManagerInitExceptionNotifier
import org.owntracks.android.ui.navigation.BottomNavBar
import org.owntracks.android.ui.navigation.Destination
import org.owntracks.android.ui.navigation.OwnTracksNavHost
import org.owntracks.android.ui.navigation.navigateToDestination
import org.owntracks.android.ui.preferences.PreferenceScreen
import org.owntracks.android.ui.preferences.PreferencesTopAppBar
import org.owntracks.android.ui.waypoints.WaypointsTopAppBar
import org.owntracks.android.ui.map.ContactsTopAppBar
import org.owntracks.android.ui.theme.OwnTracksTheme
import org.owntracks.android.ui.welcome.WelcomeActivity
import timber.log.Timber

@AndroidEntryPoint
class MapActivity :
    AppCompatActivity(),
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
  private var sensorManager: SensorManager? = null
  private var orientationSensor: Sensor? = null
  private var mapFragmentContainerView: FragmentContainerView? = null

  private lateinit var backPressedCallback: OnBackPressedCallback

  // Snackbar state for Compose
  private var snackbarHostState: SnackbarHostState? = null

  @Inject lateinit var notificationsStash: NotificationsStash

  @Inject lateinit var contactImageBindingAdapter: ContactImageBindingAdapter

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

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    EntryPointAccessors.fromActivity(this, MapActivityEntryPoint::class.java).let {
      supportFragmentManager.fragmentFactory = it.fragmentFactory
    }

    super.onCreate(savedInstanceState)

    if (!preferences.setupCompleted) {
      startActivity(Intent(this, WelcomeActivity::class.java))
      finish()
      return
    }

    setContent {
      OwnTracksTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val scope = rememberCoroutineScope()

        // Observe state from ViewModel
        val monitoringMode by viewModel.currentMonitoringMode.observeAsState(MonitoringMode.Significant)
        val currentLocation by viewModel.currentLocation.observeAsState()
        val sendingLocation by viewModel.sendingLocation.observeAsState(false)

        // Sync status state
        val endpointState by viewModel.endpointState.collectAsStateWithLifecycle()
        val queueLength by viewModel.queueLength.collectAsStateWithLifecycle()
        val lastSuccessfulSync by viewModel.lastSuccessfulSync.collectAsStateWithLifecycle()

        // Send location when GPS fix becomes available while waiting
        LaunchedEffect(currentLocation, sendingLocation) {
          if (sendingLocation && currentLocation != null) {
            viewModel.onLocationAvailableWhileSending(currentLocation!!)
          }
        }

        // State for monitoring mode bottom sheet
        var showMonitoringSheet by remember { mutableStateOf(false) }

        // State for sync status dialog
        var showSyncStatusDialog by remember { mutableStateOf(false) }

        // State for waypoints menu and export trigger
        var showWaypointsMenu by remember { mutableStateOf(false) }
        var triggerWaypointsExport by remember { mutableStateOf(false) }

        // State for preferences sub-screen navigation
        var preferencesCurrentScreen by rememberSaveable(stateSaver = PreferenceScreen.Saver) {
          mutableStateOf(PreferenceScreen.Root)
        }

        // Snackbar state
        val snackbarState = remember { SnackbarHostState() }
        snackbarHostState = snackbarState

        // Toast state
        val toastState = rememberToastState()

        // Determine current destination for bottom nav highlighting
        val currentDestination = when (currentRoute) {
          Destination.Contacts.route -> Destination.Contacts
          Destination.Waypoints.route -> Destination.Waypoints
          Destination.Preferences.route -> Destination.Preferences
          else -> Destination.Map
        }

        // Reset preferences screen when navigating away
        LaunchedEffect(currentDestination) {
          if (currentDestination != Destination.Preferences) {
            preferencesCurrentScreen = PreferenceScreen.Root
          }
        }

        // Collect location request events for snackbar
        LaunchedEffect(Unit) {
          viewModel.locationRequestContactCommandFlow.collect { contact ->
            snackbarState.showSnackbar(
                getString(R.string.requestLocationSent, contact.displayName)
            )
          }
        }

        // Show toast when location report is triggered
        LaunchedEffect(Unit) {
          viewModel.locationSentFlow.collect {
            toastState.show(getString(R.string.publishQueued))
          }
        }

        Scaffold(
            topBar = {
              when (currentDestination) {
                Destination.Map -> {
                  MapTopAppBar(
                      monitoringMode = monitoringMode,
                      sendingLocation = sendingLocation,
                      endpointState = endpointState,
                      queueLength = queueLength,
                      onMonitoringClick = { showMonitoringSheet = true },
                      onReportClick = { viewModel.sendLocation() },
                      onSyncStatusClick = { showSyncStatusDialog = true }
                  )
                }
                Destination.Contacts -> {
                  ContactsTopAppBar()
                }
                Destination.Waypoints -> {
                  WaypointsTopAppBar(
                      onAddClick = {
                        startActivity(Intent(this@MapActivity, org.owntracks.android.ui.waypoint.WaypointActivity::class.java))
                      },
                      showMenu = showWaypointsMenu,
                      onShowMenu = { showWaypointsMenu = true },
                      onDismissMenu = { showWaypointsMenu = false },
                      onImportClick = {
                        showWaypointsMenu = false
                        startActivity(Intent(this@MapActivity, org.owntracks.android.ui.preferences.load.LoadActivity::class.java))
                      },
                      onExportClick = {
                        showWaypointsMenu = false
                        triggerWaypointsExport = true
                      }
                  )
                }
                Destination.Preferences -> {
                  PreferencesTopAppBar(
                      currentScreen = preferencesCurrentScreen,
                      onBackClick = { preferencesCurrentScreen = PreferenceScreen.Root }
                  )
                }
                else -> {}
              }
            },
            bottomBar = {
              BottomNavBar(
                  currentDestination = currentDestination,
                  onNavigate = { destination ->
                    navController.navigateToDestination(destination)
                  }
              )
            },
            snackbarHost = { SnackbarHost(snackbarState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
          Box(
              modifier = Modifier
                  .fillMaxSize()
                  .padding(paddingValues)
          ) {
            // MapFragment hosted via AndroidView - only visible when on Map destination
            AndroidView(
                factory = { context ->
                  FragmentContainerView(context).apply {
                    id = R.id.mapFragment
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    mapFragmentContainerView = this
                    // Add the map fragment immediately after the container is created
                    post {
                      if (supportFragmentManager.findFragmentById(R.id.mapFragment) == null) {
                        val mapFragment =
                            supportFragmentManager.fragmentFactory.instantiate(
                                this@MapActivity.classLoader,
                                MapFragment::class.java.name,
                            )
                        supportFragmentManager.commit(true) { replace(R.id.mapFragment, mapFragment, "map") }
                      }
                    }
                  }
                },
                update = { view ->
                  view.visibility = if (currentDestination == Destination.Map) View.VISIBLE else View.INVISIBLE
                },
                modifier = Modifier.fillMaxSize()
            )

            // NavHost for screen content (overlays on top of map when not on Map destination)
            OwnTracksNavHost(
                navController = navController,
                startDestination = Destination.Map.route,
                onContactSelected = { contact ->
                  viewModel.setLiveContact(contact.id)
                },
                preferencesCurrentScreen = preferencesCurrentScreen,
                onPreferencesNavigateToScreen = { preferencesCurrentScreen = it },
                triggerWaypointsExport = triggerWaypointsExport,
                onWaypointsExportTriggered = { triggerWaypointsExport = false },
                modifier = Modifier.fillMaxSize()
            )

            // Map-specific UI (FABs and bottom sheet) - only shown when on Map destination
            if (currentDestination == Destination.Map) {
              MapOverlayContent(
                  viewModel = viewModel,
                  contactImageBindingAdapter = contactImageBindingAdapter,
                  sensorManager = sensorManager,
                  orientationSensor = orientationSensor,
                  onCheckLocationPermissions = { explicit ->
                    checkAndRequestLocationPermissions(explicit)
                  },
                  onCheckLocationServices = { explicit ->
                    checkAndRequestLocationServicesEnabled(explicit)
                  },
                  onShowMapLayersDialog = {
                    MapLayerBottomSheetDialog().show(supportFragmentManager, "layerBottomSheetDialog")
                  },
                  onNavigateToContact = { navigateToCurrentContact(scope, snackbarState) },
                  onShareContact = { shareCurrentContact() }
              )
            }
          }
        }

        // Monitoring mode bottom sheet
        if (showMonitoringSheet) {
          MonitoringModeBottomSheet(
              onDismiss = { showMonitoringSheet = false },
              onModeSelected = { mode ->
                viewModel.setMonitoringMode(mode)
                showMonitoringSheet = false
              }
          )
        }

        // Sync status dialog
        if (showSyncStatusDialog) {
          SyncStatusDialog(
              endpointState = endpointState,
              queueLength = queueLength,
              lastSuccessfulSync = lastSuccessfulSync,
              onDismiss = { showSyncStatusDialog = false },
              onSyncNow = { viewModel.triggerSync() }
          )
        }

        // Custom toast overlay
        CustomToastHost(toastState = toastState)
      }
    }

    backPressedCallback =
        onBackPressedDispatcher.addCallback(this, false) {
          viewModel.onClearContactClicked()
        }

    viewModel.apply {
      currentContact.observe(this@MapActivity) { contact: Contact? ->
        backPressedCallback.isEnabled = contact != null
      }
      currentLocation.observe(this@MapActivity) { location ->
        if (location != null) {
          updateActiveContactDistanceAndBearing(location)
        }
      }
    }

    startService(this)

    // We've been started in the foreground, so cancel the background restriction notification
    NotificationManagerCompat.from(this).cancel(BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG, 0)

    notifyOnWorkManagerInitFailure(this)
  }

  private fun shareCurrentContact() {
    viewModel.currentContact.value?.let { contact ->
      startActivity(
          Intent.createChooser(
              Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    getString(
                        R.string.shareContactBody,
                        contact.displayName,
                        contact.geocodedLocation,
                        contact.latLng?.toDisplayString() ?: "",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochSecond(contact.locationTimestamp)),
                    ),
                )
              },
              "Share Location",
          ),
      )
    }
  }

  private fun navigateToCurrentContact(
      scope: kotlinx.coroutines.CoroutineScope,
      snackbarState: SnackbarHostState
  ) {
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
        scope.launch {
          snackbarState.showSnackbar(getString(R.string.noNavigationApp))
        }
      }
    }
        ?: run {
          scope.launch {
            snackbarState.showSnackbar(getString(R.string.contactLocationUnknown))
          }
        }
  }

  private fun showSnackbar(message: String) {
    lifecycleScope.launch {
      snackbarHostState?.showSnackbar(message)
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
   * User has declined to enable location permissions. [showSnackbar] the user with the option of trying
   * again (in case they didn't mean to).
   */
  private fun locationPermissionDenied(@Suppress("UNUSED_PARAMETER") code: Int) {
    Timber.d("Location Permission denied. Showing snackbar")
    preferences.userDeclinedEnableLocationPermissions = true
    // TODO: Add action to snackbar for Compose
    showSnackbar(getString(R.string.locationPermissionNotGrantedNotification))
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
    super.onResume()
    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    sensorManager?.let {
      orientationSensor = it.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
      orientationSensor?.run { Timber.d("Got a rotation vector sensor") }
    }
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

  override fun onPause() {
    super.onPause()
    sensorManager?.unregisterListener(viewModel.orientationSensorEventListener)
  }

  private fun handleIntentExtras(intent: Intent) {
    Timber.v("handleIntentExtras")
    val b = if (intent.hasExtra("_args")) intent.getBundleExtra("_args") else Bundle()
    if (b != null) {
      Timber.v("intent has extras with contact ID")
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
