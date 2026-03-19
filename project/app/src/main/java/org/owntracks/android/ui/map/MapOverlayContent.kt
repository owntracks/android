package org.owntracks.android.ui.map

import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_UI
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.data.EndpointState
import org.owntracks.android.support.ContactImageBindingAdapter

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
 * TopAppBar for the Map screen with connection status and report buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapTopAppBar(
    sendingLocation: Boolean,
    endpointState: EndpointState,
    queueLength: Int,
    onReportClick: () -> Unit,
    onConnectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val onErrorContainerColor = MaterialTheme.colorScheme.onErrorContainer

    val isSyncError = endpointState == EndpointState.ERROR ||
        endpointState == EndpointState.ERROR_CONFIGURATION
    val isDisconnected = endpointState == EndpointState.DISCONNECTED

    val syncIconTint = when {
        isSyncError -> onErrorContainerColor
        isDisconnected -> onPrimaryColor.copy(alpha = 0.5f)
        else -> onPrimaryColor
    }

    TopAppBar(
        title = { Text(stringResource(R.string.title_activity_map)) },
        actions = {
            // Connection status icon button - navigates to connection settings
            val connectionIconRes = when (endpointState) {
                EndpointState.CONNECTED -> if (queueLength > 0) R.drawable.cloud_upload else R.drawable.cloud_done
                EndpointState.IDLE -> R.drawable.cloud_off
                else -> R.drawable.cloud_alert
            }

            IconButton(onClick = onConnectionClick) {
                Icon(
                    painter = painterResource(connectionIconRes),
                    contentDescription = stringResource(R.string.connectionStatusContentDescription),
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
                        painter = painterResource(R.drawable.ic_add_location_alt),
                        contentDescription = stringResource(R.string.publish),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    )
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
    val currentContact by viewModel.currentContact.collectAsStateWithLifecycle()
    val bottomSheetHidden by viewModel.bottomSheetHidden.collectAsStateWithLifecycle()
    val contactDistance by viewModel.contactDistance.collectAsStateWithLifecycle()
    val contactBearing by viewModel.contactBearing.collectAsStateWithLifecycle()
    val relativeContactBearing by viewModel.relativeContactBearing.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val myLocationStatus by viewModel.myLocationStatus.collectAsStateWithLifecycle()
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
