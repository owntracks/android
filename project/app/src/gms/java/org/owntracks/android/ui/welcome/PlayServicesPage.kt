package org.owntracks.android.ui.welcome

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.launch
import org.owntracks.android.R

/**
 * GMS-specific page for checking Google Play Services availability
 */
@Composable
fun PlayServicesPage(
    onCanProceed: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val googleApi = remember { GoogleApiAvailability.getInstance() }

    var playServicesAvailable by remember { mutableStateOf(false) }
    var fixAvailable by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    fun checkPlayServices() {
        val result = googleApi.isGooglePlayServicesAvailable(context)
        when (result) {
            ConnectionResult.SUCCESS -> {
                playServicesAvailable = true
                fixAvailable = false
                statusMessage = context.getString(R.string.play_services_now_available)
                onCanProceed(true)
            }
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
            ConnectionResult.SERVICE_UPDATING -> {
                playServicesAvailable = false
                fixAvailable = true
                statusMessage = context.getString(R.string.play_services_update_required)
                onCanProceed(false)
            }
            else -> {
                playServicesAvailable = false
                fixAvailable = googleApi.isUserResolvableError(result)
                statusMessage = context.getString(R.string.play_services_not_available)
                onCanProceed(false)
            }
        }
    }

    // Check on composition
    LaunchedEffect(Unit) {
        checkPlayServices()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = stringResource(R.string.icon_description_warning),
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.welcome_play_heading),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.welcome_play_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = if (playServicesAvailable) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (fixAvailable && activity != null) {
            OutlinedButton(
                onClick = {
                    val result = googleApi.isGooglePlayServicesAvailable(context)
                    if (!googleApi.showErrorDialogFragment(
                            activity,
                            result,
                            PLAY_SERVICES_RESOLUTION_REQUEST
                        )
                    ) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.play_services_not_available)
                            )
                        }
                    }
                    // Re-check after dialog
                    checkPlayServices()
                }
            ) {
                Text(stringResource(R.string.welcomeFixIssue))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

const val PLAY_SERVICES_RESOLUTION_REQUEST = 1
