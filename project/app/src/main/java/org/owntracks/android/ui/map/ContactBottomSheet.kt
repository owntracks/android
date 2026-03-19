package org.owntracks.android.ui.map

import android.graphics.Bitmap
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.owntracks.android.R
import org.owntracks.android.model.Contact
import org.owntracks.android.support.ContactImageBindingAdapter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactBottomSheet(
    contact: Contact,
    contactDistance: Float,
    contactBearing: Float,
    relativeContactBearing: Float,
    hasCurrentLocation: Boolean,
    contactImageBindingAdapter: ContactImageBindingAdapter,
    onDismiss: () -> Unit,
    onRequestLocation: () -> Unit,
    onNavigate: () -> Unit,
    onClear: () -> Unit,
    onShare: () -> Unit,
    onPeekClick: () -> Unit,
    onPeekLongClick: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        ContactBottomSheetContent(
            contact = contact,
            contactDistance = contactDistance,
            contactBearing = contactBearing,
            relativeContactBearing = relativeContactBearing,
            hasCurrentLocation = hasCurrentLocation,
            contactImageBindingAdapter = contactImageBindingAdapter,
            onRequestLocation = onRequestLocation,
            onNavigate = onNavigate,
            onClear = onClear,
            onShare = onShare,
            onPeekClick = onPeekClick,
            onPeekLongClick = onPeekLongClick
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactBottomSheetContent(
    contact: Contact,
    contactDistance: Float,
    contactBearing: Float,
    relativeContactBearing: Float,
    hasCurrentLocation: Boolean,
    contactImageBindingAdapter: ContactImageBindingAdapter,
    onRequestLocation: () -> Unit,
    onNavigate: () -> Unit,
    onClear: () -> Unit,
    onShare: () -> Unit,
    onPeekClick: () -> Unit,
    onPeekLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Contact Peek Row
        ContactPeekRow(
            contact = contact,
            contactImageBindingAdapter = contactImageBindingAdapter,
            onClick = onPeekClick,
            onLongClick = onPeekLongClick
        )

        if (contact.latLng != null) {
            HorizontalDivider()

            // Contact Details Grid
            ContactDetailsGrid(
                contact = contact,
                contactDistance = contactDistance,
                contactBearing = contactBearing,
                relativeContactBearing = relativeContactBearing,
                hasCurrentLocation = hasCurrentLocation
            )

            HorizontalDivider()
        }

        // Additional Info
        ContactInfoSection(contact = contact)

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                OutlinedButton(onClick = onRequestLocation) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.menuContactRequestLocation))
                }

                if (contact.latLng != null) {
                    OutlinedButton(onClick = onNavigate) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.menuContactNavigate))
                    }
                }

                OutlinedButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.menuClear))
                }
            }

            if (contact.latLng != null) {
                Spacer(modifier = Modifier.width(16.dp))
                FloatingActionButton(
                    onClick = onShare,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactPeekRow(
    contact: Contact,
    contactImageBindingAdapter: ContactImageBindingAdapter,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ContactAvatar(
            contact = contact,
            contactImageBindingAdapter = contactImageBindingAdapter,
            modifier = Modifier.size(40.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (contact.locationTimestamp > 0) {
                    Text(
                        text = getRelativeTimeSpan(contact.locationTimestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                text = contact.geocodedLocation ?: stringResource(R.string.na),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ContactAvatar(
    contact: Contact,
    contactImageBindingAdapter: ContactImageBindingAdapter,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, contact, contact.face) {
        value = contactImageBindingAdapter.getBitmapFromCache(contact)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = stringResource(R.string.contact_image),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val initials = contact.trackerId.take(2).uppercase()
            Text(
                text = initials,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ContactDetailsGrid(
    contact: Contact,
    contactDistance: Float,
    contactBearing: Float,
    relativeContactBearing: Float,
    hasCurrentLocation: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Column 1: Accuracy, Altitude
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContactDetailItem(
                icon = painterResource(R.drawable.ic_baseline_my_location_24),
                title = stringResource(R.string.contactDetailsAccuracy),
                value = stringResource(R.string.contactDetailsAccuracyValue, contact.locationAccuracy)
            )
            ContactDetailItem(
                icon = painterResource(R.drawable.ic_baseline_airplanemode_active_24),
                title = stringResource(R.string.contactDetailsAltitude),
                value = stringResource(R.string.contactDetailsAltitudeValue, contact.altitude)
            )
        }

        // Column 2: Battery, Speed
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContactDetailItem(
                icon = painterResource(R.drawable.ic_baseline_battery_std_24),
                title = stringResource(R.string.contactDetailsBattery),
                value = contact.battery?.let {
                    stringResource(R.string.contactDetailsBatteryValue, it)
                } ?: stringResource(R.string.na)
            )
            ContactDetailItem(
                icon = painterResource(R.drawable.ic_baseline_speed_24),
                title = stringResource(R.string.contactDetailsSpeed),
                value = stringResource(R.string.contactDetailsSpeedValue, contact.velocity)
            )
        }

        // Column 3: Distance, Bearing (only if we have current location)
        if (hasCurrentLocation) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val distanceUnit = if (contactDistance > 1000f) {
                    stringResource(R.string.contactDetailsDistanceUnitKilometres)
                } else {
                    stringResource(R.string.contactDetailsDistanceUnitMeters)
                }
                val distanceValue = if (contactDistance > 1000f) {
                    contactDistance / 1000
                } else {
                    contactDistance
                }

                ContactDetailItem(
                    icon = painterResource(R.drawable.ic_baseline_architecture_24),
                    title = stringResource(R.string.contactDetailsDistance),
                    value = stringResource(R.string.contactDetailsDistanceValue, distanceValue, distanceUnit)
                )
                ContactDetailItem(
                    icon = painterResource(R.drawable.ic_baseline_arrow_upward_24),
                    iconRotation = relativeContactBearing,
                    title = stringResource(R.string.contactDetailsBearing),
                    value = stringResource(R.string.contactDetailsBearingValue, contactBearing)
                )
            }
        }
    }
}

@Composable
private fun ContactDetailItem(
    icon: Painter,
    title: String,
    value: String,
    iconRotation: Float = 0f,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(24.dp)
                .rotate(iconRotation)
        )
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContactInfoSection(
    contact: Contact,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ContactInfoRow(
            icon = painterResource(R.drawable.ic_outline_label_24),
            title = stringResource(R.string.contactDetailsTrackerId),
            value = contact.trackerId
        )
        ContactInfoRow(
            icon = painterResource(R.drawable.ic_baseline_perm_identity_24),
            title = stringResource(R.string.contactDetailsTopic),
            value = contact.id
        )
        if (contact.latLng != null) {
            ContactInfoRow(
                icon = painterResource(R.drawable.outline_location_on_24),
                title = stringResource(R.string.contactDetailsCoordinates),
                value = contact.latLng.toString()
            )
        }
    }
}

@Composable
private fun ContactInfoRow(
    icon: Painter,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getRelativeTimeSpan(timestamp: Long): String {
    return DateUtils.getRelativeTimeSpanString(
        timestamp * 1000, // Convert seconds to milliseconds
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}
