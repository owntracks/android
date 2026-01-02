package org.owntracks.android.ui.welcome

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.ui.colorScheme
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.mixins.BackgroundLocationPermissionRequester
import org.owntracks.android.ui.mixins.LocationPermissionRequester
import org.owntracks.android.ui.preferences.PreferencesActivity

abstract class BaseWelcomeActivity : ComponentActivity() {
  protected val welcomeViewModel: WelcomeViewModel by viewModels()

  @Inject lateinit var preferences: Preferences

  @Inject lateinit var requirementsChecker: RequirementsChecker

  private val locationPermissionRefresh = MutableStateFlow(0)

  private val locationPermissionRequester by
      lazy(LazyThreadSafetyMode.NONE) {
        LocationPermissionRequester(
            this,
            { _ ->
              preferences.userDeclinedEnableLocationPermissions = false
              notifyLocationPermissionsChanged()
            },
            { _ ->
              preferences.userDeclinedEnableLocationPermissions = true
              notifyLocationPermissionsChanged()
            })
      }

  private val backgroundLocationPermissionRequester =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        BackgroundLocationPermissionRequester(
            this,
            {
              preferences.userDeclinedEnableBackgroundLocationPermissions = false
              notifyLocationPermissionsChanged()
            },
            {
              preferences.userDeclinedEnableBackgroundLocationPermissions = true
              notifyLocationPermissionsChanged()
            })
      } else {
        null
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    if (preferences.setupCompleted) {
      startMapActivity(true)
      finish()
      return
    }

    onBackPressedDispatcher.addCallback(this) {
      val current = welcomeViewModel.currentFragmentPosition.value ?: 0
      if (current == 0) {
        finish()
      } else {
        welcomeViewModel.previousPage()
      }
    }

    setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      val pageDescriptors =
          remember(snackbarHostState) {
                corePages(snackbarHostState) + additionalPages(snackbarHostState)
              }
              .filter { it.visible }
      val nextEnabled by welcomeViewModel.nextEnabled.observeAsState(true)
      val doneEnabled by welcomeViewModel.doneEnabled.observeAsState(false)
      val currentPage by welcomeViewModel.currentFragmentPosition.observeAsState(0)

      MaterialTheme(colorScheme = colorScheme()) {
        WelcomeFlowScreen(
            pageDescriptors = pageDescriptors,
            currentPageIndex = currentPage,
            nextEnabled = nextEnabled,
            doneEnabled = doneEnabled,
            snackbarHostState = snackbarHostState,
            onNext = { handleNext(pageDescriptors.size) },
            onDone = { startMapActivity(clearTask = true) })
      }
    }
  }

  protected open fun additionalPages(
      @Suppress("UNUSED_PARAMETER") snackbarHostState: SnackbarHostState
  ): List<WelcomePageDescriptor> = emptyList()

  private fun handleNext(pageCount: Int) {
    if (pageCount <= 0) {
      return
    }
    val current = welcomeViewModel.currentFragmentPosition.value ?: 0
    if (current < pageCount - 1) {
      welcomeViewModel.nextPage()
    }
  }

  private fun corePages(snackbarHostState: SnackbarHostState): List<WelcomePageDescriptor> {
    val showNotificationPage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val backgroundRequest =
        if (backgroundLocationPermissionRequester != null) {
          { requestBackgroundLocationPermissions() }
        } else {
          null
        }

    return buildList {
      add(WelcomePageDescriptor("intro") { IntroPage(welcomeViewModel) })
      add(
          WelcomePageDescriptor("connection") {
            ConnectionSetupPage(
                viewModel = welcomeViewModel,
                snackbarHostState = snackbarHostState,
                onLearnMore = { openDocumentationLink() })
          })
      add(
          WelcomePageDescriptor("location") {
            LocationPermissionPage(
                viewModel = welcomeViewModel,
                preferences = preferences,
                requirementsChecker = requirementsChecker,
                permissionUpdates = locationPermissionRefresh.asStateFlow(),
                onRequestLocationPermissions = ::requestForegroundLocationPermissions,
                onRequestBackgroundPermissions = backgroundRequest)
          })
      if (showNotificationPage) {
        add(
            WelcomePageDescriptor("notification") {
              NotificationPermissionPage(
                  viewModel = welcomeViewModel,
                  preferences = preferences,
                  requirementsChecker = requirementsChecker)
            })
      }
      add(
          WelcomePageDescriptor("finish") {
            FinishPage(viewModel = welcomeViewModel, onOpenPreferences = ::openPreferences)
          })
    }
  }

  private fun requestForegroundLocationPermissions() {
    locationPermissionRequester.requestLocationPermissions(
        0,
        this,
        ::shouldShowRequestPermissionRationale,
    )
  }

  private fun requestBackgroundLocationPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      backgroundLocationPermissionRequester?.requestLocationPermissions(
          this,
          ::shouldShowRequestPermissionRationale,
      )
    }
  }

  private fun notifyLocationPermissionsChanged() {
    locationPermissionRefresh.update { it + 1 }
  }

  private fun openPreferences() {
    startActivity(Intent(this, PreferencesActivity::class.java))
  }

  private fun openDocumentationLink(): Boolean {
    return try {
      startActivity(Intent(Intent.ACTION_VIEW, getString(R.string.documentationUrl).toUri()))
      true
    } catch (_: ActivityNotFoundException) {
      false
    }
  }

  private fun startMapActivity(clearTask: Boolean) {
    val intent =
        Intent(this, MapActivity::class.java).apply {
          if (clearTask) {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
          } else {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
        }
    startActivity(intent)
  }
}
