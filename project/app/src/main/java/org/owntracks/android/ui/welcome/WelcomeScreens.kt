package org.owntracks.android.ui.welcome

import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.ui.welcome.WelcomeViewModel.ProgressState

data class WelcomePageDescriptor(
    val key: String,
    val visible: Boolean = true,
    val content: @Composable () -> Unit,
)

object WelcomeTestTags {
  const val NEXT_BUTTON = "welcome_next_button"
  const val DONE_BUTTON = "welcome_done_button"
  const val LOCATION_PERMISSION_BUTTON = "welcome_location_permission_button"
  const val BACKGROUND_LOCATION_PERMISSION_BUTTON = "welcome_background_location_permission_button"
  const val NOTIFICATION_PERMISSION_BUTTON = "welcome_notification_permission_button"
  const val PLAY_SERVICES_FIX_BUTTON = "welcome_play_services_fix_button"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeFlowScreen(
    pageDescriptors: List<WelcomePageDescriptor>,
    currentPageIndex: Int,
    nextEnabled: Boolean,
    doneEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    onNext: () -> Unit,
    onDone: () -> Unit,
) {
  val pageCount = pageDescriptors.size
  val pagerState =
      rememberPagerState(
          pageCount = { pageCount.coerceAtLeast(1) },
      )
  val safePageIndex =
      if (pageCount == 0) {
        0
      } else {
        currentPageIndex.coerceIn(0, pageCount - 1)
      }

  LaunchedEffect(safePageIndex, pageCount) {
    if (pageCount > 0 && pagerState.currentPage != safePageIndex) {
      pagerState.scrollToPage(safePageIndex)
    }
  }

  Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
    Column(
        modifier =
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween) {
          if (pageCount == 0) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center) {
                  CircularProgressIndicator()
                }
          } else {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = 24.dp)) { page ->
                  pageDescriptors[page].content()
                }
          }

          if (pageCount > 0) {
            WelcomePagerIndicator(
                pageCount = pageCount,
                currentPage = safePageIndex,
                modifier = Modifier.padding(bottom = 16.dp))
          }

          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (nextEnabled && pageCount > 0) {
              Button(
                  modifier = Modifier.fillMaxWidth().testTag(WelcomeTestTags.NEXT_BUTTON),
                  onClick = onNext) {
                    Text(text = stringResource(id = R.string.next))
                  }
            }
            if (doneEnabled) {
              Button(
                  modifier = Modifier.fillMaxWidth().testTag(WelcomeTestTags.DONE_BUTTON),
                  onClick = onDone) {
                    Text(text = stringResource(id = R.string.done_heading))
                  }
            }
          }
        }
  }
}

@Composable
private fun WelcomePagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
  Row(
      modifier = modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically) {
        repeat(pageCount) { index ->
          val alpha by
              animateFloatAsState(
                  targetValue = if (index == currentPage) 1f else 0.5f,
                  label = "pager-indicator-alpha")
          Box(
              modifier =
                  Modifier.padding(horizontal = 4.dp)
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)))
        }
      }
}

@Composable
fun WelcomePageLayout(
    iconRes: Int,
    iconContentDescription: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
  val scrollState = rememberScrollState()
  Column(
      modifier = modifier.fillMaxSize().verticalScroll(scrollState),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = iconContentDescription,
            modifier = Modifier.size(112.dp),
            contentScale = ContentScale.Fit)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
        Spacer(modifier = Modifier.height(16.dp))
        content()
        Spacer(modifier = Modifier.height(24.dp))
      }
}

@Composable
fun IntroPage(viewModel: WelcomeViewModel) {
  LaunchedEffect(Unit) { viewModel.setWelcomeState(ProgressState.PERMITTED) }
  WelcomePageLayout(
      iconRes = R.drawable.ic_owntracks_80,
      iconContentDescription = stringResource(id = R.string.icon_description_owntracks),
      title = stringResource(id = R.string.welcome_heading),
      description = stringResource(id = R.string.welcome_description))
}

@Composable
fun ConnectionSetupPage(
    viewModel: WelcomeViewModel,
    snackbarHostState: SnackbarHostState,
    onLearnMore: () -> Boolean,
) {
  val scope = rememberCoroutineScope()
  val noBrowserMessage = stringResource(id = R.string.noBrowserInstalled)
  LaunchedEffect(Unit) { viewModel.setWelcomeState(ProgressState.PERMITTED) }
  WelcomePageLayout(
      iconRes = R.drawable.ic_baseline_miscellaneous_services_24,
      iconContentDescription = stringResource(id = R.string.welcome_connection_setup_title),
      title = stringResource(id = R.string.welcome_connection_setup_title),
      description = stringResource(id = R.string.welcome_connection_setup_description)) {
        OutlinedButton(
            onClick = {
              if (!onLearnMore()) {
                scope.launch { snackbarHostState.showSnackbar(message = noBrowserMessage) }
              }
            }) {
              Text(
                  text =
                      stringResource(
                          id = R.string.welcome_connection_setup_learn_more_button_label))
            }
      }
}

private data class LocationPermissionUiState(
    val hasForegroundPermission: Boolean,
    val hasBackgroundPermission: Boolean,
    val userDeclinedForeground: Boolean,
    val userDeclinedBackground: Boolean,
)

@Composable
fun LocationPermissionPage(
    viewModel: WelcomeViewModel,
    preferences: Preferences,
    requirementsChecker: RequirementsChecker,
    permissionUpdates: StateFlow<Int>,
    onRequestLocationPermissions: () -> Unit,
    onRequestBackgroundPermissions: (() -> Unit)?,
) {
  var uiState by remember {
    mutableStateOf(buildLocationPermissionState(preferences, requirementsChecker))
  }
  val refreshKey by permissionUpdates.collectAsState(initial = 0)

  val refreshState = { uiState = buildLocationPermissionState(preferences, requirementsChecker) }

  LaunchedEffect(refreshKey) { refreshState() }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshState() }

  val progressState =
      when {
        uiState.hasForegroundPermission -> ProgressState.PERMITTED
        uiState.userDeclinedForeground -> ProgressState.PERMITTED
        else -> ProgressState.NOT_PERMITTED
      }

  LaunchedEffect(progressState) { viewModel.setWelcomeState(progressState) }

  val showBackgroundButton =
      onRequestBackgroundPermissions != null &&
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
          uiState.hasForegroundPermission &&
          !uiState.hasBackgroundPermission
  val showGrantedMessage = uiState.hasForegroundPermission && uiState.hasBackgroundPermission

  WelcomePageLayout(
      iconRes = R.drawable.ic_baseline_not_listed_location_24,
      iconContentDescription = stringResource(id = R.string.welcome_location_permission_title),
      title = stringResource(id = R.string.welcome_location_permission_title),
      description = stringResource(id = R.string.welcome_location_permission_description)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              if (!uiState.hasForegroundPermission) {
                OutlinedButton(
                    modifier = Modifier.testTag(WelcomeTestTags.LOCATION_PERMISSION_BUTTON),
                    onClick = onRequestLocationPermissions) {
                      Text(text = stringResource(id = R.string.welcome_location_permission_request))
                    }
              }
              if (showBackgroundButton && onRequestBackgroundPermissions != null) {
                OutlinedButton(
                    modifier =
                        Modifier.testTag(WelcomeTestTags.BACKGROUND_LOCATION_PERMISSION_BUTTON),
                    onClick = onRequestBackgroundPermissions) {
                      Text(
                          text =
                              stringResource(
                                  id = R.string.welcome_background_location_permission_request))
                    }
              }
              if (showGrantedMessage) {
                Text(
                    text = stringResource(id = R.string.welcome_location_permission_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center)
              }
            }
      }
}

private fun buildLocationPermissionState(
    preferences: Preferences,
    requirementsChecker: RequirementsChecker
): LocationPermissionUiState {
  val hasForeground = requirementsChecker.hasLocationPermissions()
  val hasBackground =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        requirementsChecker.hasBackgroundLocationPermission()
      } else {
        true
      }
  return LocationPermissionUiState(
      hasForegroundPermission = hasForeground,
      hasBackgroundPermission = hasBackground,
      userDeclinedForeground = preferences.userDeclinedEnableLocationPermissions,
      userDeclinedBackground = preferences.userDeclinedEnableBackgroundLocationPermissions)
}

private data class NotificationPermissionUiState(
    val hasPermission: Boolean,
    val userDeclined: Boolean,
)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NotificationPermissionPage(
    viewModel: WelcomeViewModel,
    preferences: Preferences,
    requirementsChecker: RequirementsChecker,
) {
  var uiState by remember {
    mutableStateOf(buildNotificationPermissionState(preferences, requirementsChecker))
  }
  val refreshState = {
    uiState = buildNotificationPermissionState(preferences, requirementsChecker)
  }
  LaunchedEffect(Unit) { refreshState() }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshState() }

  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        preferences.userDeclinedEnableNotificationPermissions = !granted
        refreshState()
      }

  val progressState =
      when {
        uiState.hasPermission -> ProgressState.PERMITTED
        uiState.userDeclined -> ProgressState.PERMITTED
        else -> ProgressState.NOT_PERMITTED
      }
  LaunchedEffect(progressState) { viewModel.setWelcomeState(progressState) }

  WelcomePageLayout(
      iconRes = R.drawable.ic_baseline_notifications_48,
      iconContentDescription = stringResource(id = R.string.welcome_notification_permission_title),
      title = stringResource(id = R.string.welcome_notification_permission_title),
      description = stringResource(id = R.string.welcome_notification_permission_description)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              if (!uiState.hasPermission) {
                OutlinedButton(
                    modifier = Modifier.testTag(WelcomeTestTags.NOTIFICATION_PERMISSION_BUTTON),
                    onClick = { launcher.launch(POST_NOTIFICATIONS) }) {
                      Text(
                          text =
                              stringResource(id = R.string.welcome_notification_permission_request))
                    }
              } else {
                Text(
                    text = stringResource(id = R.string.welcome_notification_permission_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center)
              }
            }
      }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun buildNotificationPermissionState(
    preferences: Preferences,
    requirementsChecker: RequirementsChecker
): NotificationPermissionUiState {
  return NotificationPermissionUiState(
      hasPermission = requirementsChecker.hasNotificationPermissions(),
      userDeclined = preferences.userDeclinedEnableNotificationPermissions)
}

@Composable
fun FinishPage(viewModel: WelcomeViewModel, onOpenPreferences: () -> Unit) {
  LaunchedEffect(Unit) { viewModel.setWelcomeState(ProgressState.FINISHED) }
  WelcomePageLayout(
      iconRes = R.drawable.ic_baseline_done_all_48,
      iconContentDescription = stringResource(id = R.string.icon_description_done),
      title = stringResource(id = R.string.done_heading),
      description = stringResource(id = R.string.enjoy_description)) {
        OutlinedButton(onClick = onOpenPreferences) {
          Text(text = stringResource(id = R.string.welcome_finish_open_preferences_button_label))
        }
      }
}
