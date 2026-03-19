package org.owntracks.android.ui.preferences.about

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.owntracks.android.BuildConfig
import org.owntracks.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onLicensesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val versionInfo = remember { getVersionInfo(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_activity_about)) },
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
            // Version / Changelog
            AboutItem(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.preferencesChangelog),
                summary = versionInfo,
                onClick = {
                    openUrl(context, context.getString(R.string.changelogUrl))
                }
            )

            // Documentation
            AboutItem(
                icon = Icons.Default.Description,
                title = stringResource(R.string.preferencesDocumentation),
                summary = stringResource(R.string.documentationUrl),
                onClick = {
                    openUrl(context, context.getString(R.string.documentationUrl))
                }
            )

            // License
            AboutItem(
                icon = Icons.Default.VerifiedUser,
                title = stringResource(R.string.aboutLicense),
                summary = "Eclipse Public License 1.0 (EPL 1.0)",
                onClick = {
                    openUrl(context, context.getString(R.string.licenseUrl))
                }
            )

            // Source Code / Repository
            AboutItem(
                icon = Icons.Default.Code,
                title = stringResource(R.string.preferencesRepository),
                summary = stringResource(R.string.aboutSourceCodeSummary),
                onClick = {
                    openUrl(context, context.getString(R.string.repoUrl))
                }
            )

            // Libraries / Licenses
            AboutItem(
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                title = stringResource(R.string.preferencesLicenses),
                summary = stringResource(R.string.preferencesLicensesSummary),
                onClick = onLicensesClick
            )

            // Translations
            AboutItem(
                icon = Icons.Default.Translate,
                title = stringResource(R.string.aboutTranslations),
                summary = pluralStringResource(
                    R.plurals.aboutTranslationsSummary,
                    BuildConfig.TRANSLATION_COUNT,
                    BuildConfig.TRANSLATION_COUNT
                ),
                onClick = {
                    openUrl(context, context.getString(R.string.translationContributionUrl))
                }
            )

            // Feedback Category Header
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.aboutFeedbackCategoryTitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Report an Issue
            AboutItem(
                icon = Icons.Default.BugReport,
                title = stringResource(R.string.aboutIssuesTitle),
                summary = stringResource(R.string.aboutIssuesSummary),
                onClick = {
                    openUrl(context, context.getString(R.string.issueUrl))
                }
            )

            // Mastodon
            AboutItem(
                icon = null, // Custom icon not available in Material Icons
                title = stringResource(R.string.preferencesMastodon),
                summary = stringResource(R.string.mastodonUrl),
                onClick = {
                    openUrl(context, context.getString(R.string.mastodonUrl))
                }
            )
        }
    }
}

@Composable
private fun AboutItem(
    icon: ImageVector?,
    title: String,
    summary: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
        } else {
            // Reserve space for icon alignment
            Spacer(modifier = Modifier.width(40.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getVersionInfo(context: Context): String {
    return try {
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfoCompat(context.packageName)
        val versionName = packageInfo.versionName
        @Suppress("DEPRECATION")
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
        val flavor = context.getString(R.string.aboutFlavorName)
        "${context.getString(R.string.version)} $versionName ($versionCode) - $flavor"
    } catch (e: PackageManager.NameNotFoundException) {
        context.getString(R.string.na)
    }
}

private fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION") getPackageInfo(packageName, 0)
    }

private fun openUrl(context: Context, url: String) {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, url.toUri())
    )
}
