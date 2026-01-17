package org.owntracks.android.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.Instant
import org.owntracks.android.R
import org.owntracks.android.data.EndpointState
import org.owntracks.android.support.DateFormatter

@Composable
fun SyncStatusDialog(
    endpointState: EndpointState,
    queueLength: Int,
    lastSuccessfulSync: Instant?,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sync_status_dialog_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Connection Status
                SyncStatusItem(
                    label = stringResource(R.string.sync_status_connection),
                    value = endpointState.getLabel(context)
                )

                // Error message if any
                if (endpointState.error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = endpointState.getErrorLabel(context),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Queue Length
                SyncStatusItem(
                    label = stringResource(R.string.sync_status_queue_length),
                    value = queueLength.toString()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Last Successful Sync
                SyncStatusItem(
                    label = stringResource(R.string.sync_status_last_success),
                    value = if (lastSuccessfulSync != null) {
                        DateFormatter.formatDate(lastSuccessfulSync)
                    } else {
                        stringResource(R.string.sync_status_never)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSyncNow()
                onDismiss()
            }) {
                Text(stringResource(R.string.sync_status_sync_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.sync_status_close))
            }
        },
        modifier = modifier
    )
}

@Composable
private fun SyncStatusItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
