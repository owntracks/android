package org.owntracks.android.ui.preferences.about

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.owntracks.android.R

/**
 * Data class representing a third-party library license entry
 */
data class LibraryLicense(
    val name: String,
    val author: String,
    val url: String
)

/**
 * List of all third-party libraries used in the app
 */
private val libraries = listOf(
    LibraryLicense("Conscrypt", "Google", "https://github.com/google/conscrypt"),
    LibraryLicense("OSMDroid", "OSM Map Provider", "https://github.com/osmdroid/osmdroid"),
    LibraryLicense("Dagger Hilt", "Google", "https://dagger.dev/hilt/"),
    LibraryLicense("Paho MQTTv3.1 Java Client", "Eclipse Foundation", "https://github.com/eclipse/paho.mqtt.java"),
    LibraryLicense("Okhttp3", "Square", "https://github.com/square/okhttp"),
    LibraryLicense("Jackson", "Fasterxml", "https://github.com/FasterXML/jackson"),
    LibraryLicense("Tape", "Square", "https://square.github.io/tape/"),
    LibraryLicense("Timber", "Jake Wharton", "https://github.com/JakeWharton/timber"),
    LibraryLicense("HTTP Components Core 5", "Apache", "https://hc.apache.org/httpcomponents-core-5.1.x/"),
    LibraryLicense("MaterialDrawer", "Mikepenz", "https://github.com/mikepenz/MaterialDrawer"),
    LibraryLicense("Materialize", "Mikepenz", "https://github.com/mikepenz/Materialize"),
    LibraryLicense("OpenCage Geocoder", "Reverse Geocoding API", "https://opencagedata.com/credits")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preferencesLicenses)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
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
            libraries.forEach { library ->
                LicenseItem(
                    library = library,
                    onClick = {
                        openUrl(context, library.url)
                    }
                )
            }
        }
    }
}

@Composable
private fun LicenseItem(
    library: LibraryLicense,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = library.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = library.author,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun openUrl(context: Context, url: String) {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, url.toUri())
    )
}
