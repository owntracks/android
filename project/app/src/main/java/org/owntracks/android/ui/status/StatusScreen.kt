package org.owntracks.android.ui.status

import android.content.Context
import android.content.Intent
import android.location.Location
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import org.owntracks.android.R
import org.owntracks.android.data.EndpointState
import org.owntracks.android.support.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    viewModel: StatusViewModel,
    onBackClick: () -> Unit,
    onViewLogsClick: () -> Unit,
    batteryOptimizationIntents: BatteryOptimizingIntents,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val endpointState by viewModel.endpointState.collectAsStateWithLifecycle()
    val endpointQueueLength by viewModel.endpointQueueLength.collectAsStateWithLifecycle()
    val serviceStarted by viewModel.serviceStarted.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val dozeWhitelisted by viewModel.dozeWhitelisted.observeAsState(initial = false)
    val locationPermissions by viewModel.locationPermissions.observeAsState(initial = R.string.statusLocationPermissionsUnknown)

    var showBatteryDialog by remember { mutableStateOf(false) }
    var showLocationPermissionsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_activity_status)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Endpoint State
            StatusItem(
                primary = endpointState.getLabel(context),
                secondary = stringResource(R.string.status_endpoint_state_hint)
            )

            // Endpoint State Message (only show if there's an error or message)
            if (endpointState.error != null || endpointState.message != null) {
                StatusItem(
                    primary = if (endpointState.error != null) {
                        endpointState.getErrorLabel(context)
                    } else {
                        endpointState.message ?: ""
                    },
                    secondary = stringResource(R.string.status_endpoint_state_message_hint)
                )
            }

            // Queue Length
            StatusItem(
                primary = endpointQueueLength.toString(),
                secondary = stringResource(R.string.status_endpoint_queue_hint)
            )

            // Last Background Update
            StatusItem(
                primary = formatLocationTime(currentLocation),
                secondary = stringResource(R.string.status_last_background_update_hint)
            )

            // Service Started
            StatusItem(
                primary = formatServiceStarted(serviceStarted),
                secondary = stringResource(R.string.status_background_service_started_hint)
            )

            // Battery Optimization
            StatusItem(
                primary = stringResource(
                    if (dozeWhitelisted) R.string.statusBatteryDozeWhiteListEnabled
                    else R.string.statusBatteryDozeWhiteListDisabled
                ),
                secondary = stringResource(R.string.status_battery_optimization_whitelisted_hint),
                onClick = { showBatteryDialog = true }
            )

            // Location Permissions
            StatusItem(
                primary = stringResource(locationPermissions),
                secondary = stringResource(R.string.statusLocationPermissions),
                onClick = {
                    if (locationPermissions != R.string.statusLocationPermissionsFineBackground) {
                        showLocationPermissionsDialog = true
                    } else {
                        openAppSettings(context)
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // View Logs Button
            StatusItem(
                primary = stringResource(R.string.viewLogs),
                secondary = null,
                onClick = onViewLogsClick
            )
        }
    }

    // Battery Optimization Dialog
    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = { Text(stringResource(R.string.batteryOptimizationWhitelistDialogTitle)) },
            text = { Text(stringResource(R.string.batteryOptimizationWhitelistDialogMessage)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatteryDialog = false
                        val intent = if (dozeWhitelisted) {
                            batteryOptimizationIntents.settingsIntent
                        } else {
                            batteryOptimizationIntents.directPackageIntent
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text(stringResource(R.string.batteryOptimizationWhitelistDialogButtonLabel))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Location Permissions Dialog
    if (showLocationPermissionsDialog) {
        AlertDialog(
            onDismissRequest = { showLocationPermissionsDialog = false },
            title = { Text(stringResource(R.string.statusLocationPermissionsPromptTitle)) },
            text = { Text(stringResource(R.string.statusLocationPermissionsPromptText)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationPermissionsDialog = false
                        openAppSettings(context)
                    }
                ) {
                    Text(stringResource(R.string.statusLocationPermissionsPromptPositiveButton))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationPermissionsDialog = false }) {
                    Text(stringResource(R.string.statusLocationPermissionsPromptNegativeButton))
                }
            }
        )
    }
}

@Composable
private fun StatusItem(
    primary: String,
    secondary: String?,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = primary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (secondary != null) {
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatLocationTime(location: Location?): String {
    return if (location != null && location.time != 0L) {
        DateFormatter.formatDate(location.time)
    } else {
        "N/A"
    }
}

private fun formatServiceStarted(instant: Instant): String {
    return if (instant != Instant.EPOCH) {
        DateFormatter.formatDate(instant)
    } else {
        "N/A"
    }
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
        }
    )
}
