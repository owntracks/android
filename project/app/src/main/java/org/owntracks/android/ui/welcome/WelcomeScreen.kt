package org.owntracks.android.ui.welcome

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.automirrored.filled.NotListedLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import org.owntracks.android.R

/**
 * Represents a welcome page in the onboarding flow.
 * GMS variant adds PlayServices page via extension in gms source set.
 */
sealed class WelcomePage {
    data object Intro : WelcomePage()
    data object ConnectionSetup : WelcomePage()
    data object LocationPermission : WelcomePage()
    data object NotificationPermission : WelcomePage()
    data object PlayServices : WelcomePage()
    data object Finish : WelcomePage()
}

/**
 * Main Welcome screen with HorizontalPager
 */
@Composable
fun WelcomeScreen(
    pages: List<WelcomePage>,
    hasLocationPermissions: () -> Boolean,
    hasBackgroundLocationPermission: () -> Boolean,
    hasNotificationPermissions: () -> Boolean,
    onLocationPermissionGranted: () -> Unit,
    onLocationPermissionDenied: () -> Unit,
    onBackgroundLocationPermissionGranted: () -> Unit,
    onBackgroundLocationPermissionDenied: () -> Unit,
    onNotificationPermissionGranted: () -> Unit,
    onNotificationPermissionDenied: () -> Unit,
    onSetupComplete: () -> Unit,
    onOpenPreferences: () -> Unit,
    userDeclinedLocationPermission: Boolean,
    userDeclinedNotificationPermission: Boolean,
    // Play Services page content (GMS only)
    playServicesPageContent: (@Composable (snackbarHostState: SnackbarHostState, onCanProceed: (Boolean) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Track if current page allows progression
    var canProceed by remember { mutableStateOf(true) }
    val isLastPage = pagerState.currentPage == pages.size - 1

    // Update canProceed based on current page
    LaunchedEffect(pagerState.currentPage) {
        canProceed = when (pages[pagerState.currentPage]) {
            is WelcomePage.LocationPermission ->
                hasLocationPermissions() || userDeclinedLocationPermission
            is WelcomePage.NotificationPermission ->
                hasNotificationPermissions() || userDeclinedNotificationPermission
            is WelcomePage.PlayServices -> false // PlayServices page controls this via callback
            else -> true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { pageIndex ->
                when (pages[pageIndex]) {
                    is WelcomePage.Intro -> IntroPage()
                    is WelcomePage.ConnectionSetup -> ConnectionSetupPage(
                        snackbarHostState = snackbarHostState
                    )
                    is WelcomePage.LocationPermission -> LocationPermissionPage(
                        hasLocationPermissions = hasLocationPermissions,
                        hasBackgroundLocationPermission = hasBackgroundLocationPermission,
                        onPermissionGranted = {
                            onLocationPermissionGranted()
                            canProceed = true
                        },
                        onPermissionDenied = {
                            onLocationPermissionDenied()
                            canProceed = true
                        },
                        onBackgroundPermissionGranted = onBackgroundLocationPermissionGranted,
                        onBackgroundPermissionDenied = onBackgroundLocationPermissionDenied
                    )
                    is WelcomePage.NotificationPermission -> NotificationPermissionPage(
                        hasNotificationPermissions = hasNotificationPermissions,
                        onPermissionGranted = {
                            onNotificationPermissionGranted()
                            canProceed = true
                        },
                        onPermissionDenied = {
                            onNotificationPermissionDenied()
                            canProceed = true
                        }
                    )
                    is WelcomePage.PlayServices -> {
                        playServicesPageContent?.invoke(snackbarHostState) { canProceedNow ->
                            canProceed = canProceedNow
                        } ?: Box(Modifier.fillMaxSize())
                    }
                    is WelcomePage.Finish -> FinishPage(
                        onOpenPreferences = {
                            onSetupComplete()
                            onOpenPreferences()
                        }
                    )
                }
            }

            // Next/Done buttons
            if (isLastPage) {
                Button(
                    onClick = onSetupComplete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.done_heading))
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    enabled = canProceed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (canProceed) 1f else 0f)
                ) {
                    Text(stringResource(R.string.next))
                }
            }

            // Page indicator
            PageIndicator(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .size(12.dp)
                    .alpha(if (index == currentPage) 1f else 0.5f)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun IntroPage(
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Image(
            painter = painterResource(R.drawable.ic_owntracks_80),
            contentDescription = stringResource(R.string.icon_description_owntracks),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.welcome_heading),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.welcome_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun ConnectionSetupPage(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val noBrowserMessage = stringResource(R.string.noBrowserInstalled)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = Icons.Default.MiscellaneousServices,
            contentDescription = stringResource(R.string.welcome_connection_setup_title),
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.welcome_connection_setup_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.welcome_connection_setup_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = {
                try {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            context.getString(R.string.documentationUrl).toUri()
                        )
                    )
                } catch (e: ActivityNotFoundException) {
                    scope.launch {
                        snackbarHostState.showSnackbar(noBrowserMessage)
                    }
                }
            }
        ) {
            Text(stringResource(R.string.welcome_connection_setup_learn_more_button_label))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LocationPermissionPage(
    hasLocationPermissions: () -> Boolean,
    hasBackgroundLocationPermission: () -> Boolean,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onBackgroundPermissionGranted: () -> Unit,
    onBackgroundPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    var locationGranted by remember { mutableStateOf(hasLocationPermissions()) }
    var backgroundGranted by remember { mutableStateOf(hasBackgroundLocationPermission()) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationGranted = granted
        if (granted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        backgroundGranted = granted
        if (granted) {
            onBackgroundPermissionGranted()
        } else {
            onBackgroundPermissionDenied()
        }
    }

    // Update state on resume
    LaunchedEffect(Unit) {
        locationGranted = hasLocationPermissions()
        backgroundGranted = hasBackgroundLocationPermission()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.NotListedLocation,
            contentDescription = stringResource(R.string.welcome_location_permission_title),
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.welcome_location_permission_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.welcome_location_permission_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        when {
            // Both permissions granted
            locationGranted && backgroundGranted -> {
                Text(
                    text = stringResource(R.string.welcome_location_permission_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // Location granted, need background
            locationGranted && !backgroundGranted -> {
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            backgroundPermissionLauncher.launch(
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.welcome_background_location_permission_request))
                }
            }
            // Need location permission
            else -> {
                OutlinedButton(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.welcome_location_permission_request))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun NotificationPermissionPage(
    hasNotificationPermissions: () -> Boolean,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    var permissionGranted by remember { mutableStateOf(hasNotificationPermissions()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    // Update state on resume
    LaunchedEffect(Unit) {
        permissionGranted = hasNotificationPermissions()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = stringResource(R.string.welcome_notification_permission_title),
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.welcome_notification_permission_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.welcome_notification_permission_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        if (permissionGranted) {
            Text(
                text = stringResource(R.string.welcome_notification_permission_granted),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            ) {
                Text(stringResource(R.string.welcome_notification_permission_request))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FinishPage(
    onOpenPreferences: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = Icons.Default.Done,
            contentDescription = stringResource(R.string.icon_description_done),
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.done_heading),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.enjoy_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(onClick = onOpenPreferences) {
            Text(stringResource(R.string.welcome_finish_open_preferences_button_label))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
