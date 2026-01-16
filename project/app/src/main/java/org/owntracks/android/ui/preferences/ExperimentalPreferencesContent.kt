package org.owntracks.android.ui.preferences

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences

@Composable
@Suppress("UNUSED_PARAMETER")
fun ExperimentalPreferencesContent(
    preferences: Preferences,
    modifier: Modifier = Modifier
) {
    // Currently the experimental screen is empty
    // Future experimental features can be added here
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.noExperimentalFeatures),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
