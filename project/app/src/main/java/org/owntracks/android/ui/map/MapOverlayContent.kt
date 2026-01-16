package org.owntracks.android.ui.map

import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_UI
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.data.EndpointState
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.support.ContactImageBindingAdapter
import timber.log.Timber

/**
 * TopAppBar for the Contacts screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsTopAppBar(
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(R.string.title_activity_contacts)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    )
}

/**
 * TopAppBar for the Map screen with monitoring mode and report buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapTopAppBar(
    monitoringMode: MonitoringMode,
    sendingLocation: Boolean,
    endpointState: EndpointState,
    queueLength: Int,
    onMonitoringClick: () -> Unit,
    onReportClick: () -> Unit,
    onSyncStatusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monitoringIcon = when (monitoringMode) {
        MonitoringMode.Quiet -> R.drawable.ic_baseline_stop_36
        MonitoringMode.Manual -> R.drawable.ic_baseline_pause_36
        MonitoringMode.Significant -> R.drawable.ic_baseline_play_arrow_36
        MonitoringMode.Move -> R.drawable.ic_step_forward_2
    }

    // Use shorter titles for accessibility
    val monitoringTitle = when (monitoringMode) {
        MonitoringMode.Quiet -> R.string.monitoringModeDialogQuietTitle
        MonitoringMode.Manual -> R.string.monitoringModeDialogManualTitle
        MonitoringMode.Significant -> R.string.monitoringModeDialogSignificantTitle
        MonitoringMode.Move -> R.string.monitoringModeDialogMoveTitle
    }

    // Color logging
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val onErrorContainerColor = MaterialTheme.colorScheme.onErrorContainer

    val isSyncError = endpointState == EndpointState.ERROR ||
        endpointState == EndpointState.ERROR_CONFIGURATION
    val isDisconnected = endpointState == EndpointState.DISCONNECTED

    // Use onErrorContainer for error state - it's darker and contrasts better with primary background
    val syncIconTint = when {
        isSyncError -> onErrorContainerColor
        isDisconnected -> onPrimaryColor.copy(alpha = 0.5f)
        else -> onPrimaryColor
    }

    LaunchedEffect(endpointState) {
        Timber.d("MapTopAppBar colors - TopBar background (primary): #${Integer.toHexString(primaryColor.toArgb())}")
        Timber.d("MapTopAppBar colors - onPrimary: #${Integer.toHexString(onPrimaryColor.toArgb())}")
        Timber.d("MapTopAppBar colors - onErrorContainer: #${Integer.toHexString(onErrorContainerColor.toArgb())}")
        Timber.d("MapTopAppBar colors - endpointState: $endpointState, isSyncError: $isSyncError, isDisconnected: $isDisconnected")
        Timber.d("MapTopAppBar colors - syncIconTint: #${Integer.toHexString(syncIconTint.toArgb())}")
    }

    TopAppBar(
        title = { },
        navigationIcon = {
            // Monitoring mode icon button
            IconButton(onClick = onMonitoringClick) {
                Icon(
                    painter = painterResource(monitoringIcon),
                    contentDescription = stringResource(monitoringTitle),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        actions = {
            // Sync status icon button
            val isSynced = queueLength == 0 && (endpointState == EndpointState.CONNECTED ||
                endpointState == EndpointState.IDLE)

            IconButton(onClick = onSyncStatusClick) {
                Icon(
                    imageVector = if (isSynced) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                    contentDescription = stringResource(R.string.sync_status_content_description),
                    tint = syncIconTint
                )
            }

            // Send location icon button
            IconButton(
                onClick = onReportClick,
                enabled = !sendingLocation
            ) {
                if (sendingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_publish_24),
                        contentDescription = stringResource(R.string.publish),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
    )
}

/**
 * Bottom sheet for selecting monitoring mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringModeBottomSheet(
    onDismiss: () -> Unit,
    onModeSelected: (MonitoringMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.monitoringModeDialogTitle),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            )

            // First row: Significant and Move
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MonitoringModeOption(
                    iconRes = R.drawable.ic_baseline_play_arrow_36,
                    title = stringResource(R.string.monitoringModeDialogSignificantTitle),
                    description = stringResource(R.string.monitoringModeDialogSignificantDescription),
                    onClick = { onModeSelected(MonitoringMode.Significant) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                MonitoringModeOption(
                    iconRes = R.drawable.ic_step_forward_2,
                    title = stringResource(R.string.monitoringModeDialogMoveTitle),
                    description = stringResource(R.string.monitoringModeDialogMoveDescription),
                    onClick = { onModeSelected(MonitoringMode.Move) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            // Second row: Manual and Quiet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MonitoringModeOption(
                    iconRes = R.drawable.ic_baseline_pause_36,
                    title = stringResource(R.string.monitoringModeDialogManualTitle),
                    description = stringResource(R.string.monitoringModeDialogManualDescription),
                    onClick = { onModeSelected(MonitoringMode.Manual) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                MonitoringModeOption(
                    iconRes = R.drawable.ic_baseline_stop_36,
                    title = stringResource(R.string.monitoringModeDialogQuietTitle),
                    description = stringResource(R.string.monitoringModeDialogQuietDescription),
                    onClick = { onModeSelected(MonitoringMode.Quiet) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun MonitoringModeOption(
    iconRes: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Overlay content for the Map screen including FABs and contact bottom sheet.
 * This is displayed on top of the MapFragment when the Map destination is active.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapOverlayContent(
    viewModel: MapViewModel,
    contactImageBindingAdapter: ContactImageBindingAdapter,
    sensorManager: SensorManager?,
    orientationSensor: Sensor?,
    onCheckLocationPermissions: (Boolean) -> MapActivity.CheckPermissionsResult,
    onCheckLocationServices: (Boolean) -> Boolean,
    onShowMapLayersDialog: () -> Unit,
    onNavigateToContact: () -> Unit,
    onShareContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentContact by viewModel.currentContact.observeAsState()
    val bottomSheetHidden by viewModel.bottomSheetHidden.observeAsState(true)
    val contactDistance by viewModel.contactDistance.observeAsState(0f)
    val contactBearing by viewModel.contactBearing.observeAsState(0f)
    val relativeContactBearing by viewModel.relativeContactBearing.observeAsState(0f)
    val currentLocation by viewModel.currentLocation.observeAsState()
    val myLocationStatus by viewModel.myLocationStatus.observeAsState(MyLocationStatus.DISABLED)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        // FABs positioned at bottom-end
        MapFabs(
            myLocationStatus = myLocationStatus,
            myLocationEnabled = currentLocation != null,
            onMyLocationClick = {
                if (onCheckLocationPermissions(true) ==
                    MapActivity.CheckPermissionsResult.HAS_PERMISSIONS) {
                    onCheckLocationServices(true)
                }
                if (viewModel.myLocationStatus.value != MyLocationStatus.DISABLED) {
                    viewModel.onMyLocationClicked()
                }
            },
            onMapLayersClick = onShowMapLayersDialog,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
        )

        // Contact bottom sheet
        currentContact?.let { contact ->
            if (!bottomSheetHidden) {
                ContactBottomSheet(
                    contact = contact,
                    contactDistance = contactDistance,
                    contactBearing = contactBearing,
                    relativeContactBearing = relativeContactBearing,
                    hasCurrentLocation = currentLocation != null,
                    contactImageBindingAdapter = contactImageBindingAdapter,
                    sheetState = sheetState,
                    onDismiss = {
                        viewModel.onClearContactClicked()
                    },
                    onRequestLocation = {
                        viewModel.sendLocationRequestToCurrentContact()
                    },
                    onNavigate = onNavigateToContact,
                    onClear = {
                        viewModel.onClearContactClicked()
                    },
                    onShare = onShareContact,
                    onPeekClick = {
                        // Expand the bottom sheet
                        scope.launch {
                            sheetState.expand()
                        }
                        // Register sensor for bearing updates
                        orientationSensor?.let { sensor ->
                            sensorManager?.registerListener(
                                viewModel.orientationSensorEventListener,
                                sensor,
                                SENSOR_DELAY_UI
                            )
                        }
                    },
                    onPeekLongClick = {
                        viewModel.onBottomSheetLongClick()
                    }
                )
            }
        }
    }
}
