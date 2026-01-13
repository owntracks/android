package org.owntracks.android.ui.status.logs

import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.owntracks.android.R
import org.owntracks.android.logging.LogEntry

// Color palette for log levels
private val LogDebugColor = Color(0xFF888888)
private val LogInfoColor = Color(0xFF00AA00)
private val LogWarningColor = Color(0xFFAAAA00)
private val LogErrorColor = Color(0xFFAA0000)
private val LogDefaultColor = Color(0xFF1976D2) // primary color approximation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    logEntries: List<LogEntry>,
    isDebugEnabled: Boolean,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onClearClick: () -> Unit,
    onToggleDebug: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var menuExpanded by remember { mutableStateOf(false) }
    var wasAtBottom by remember { mutableStateOf(true) }

    // Auto-scroll to bottom when new items are added and user was at bottom
    LaunchedEffect(logEntries.size) {
        if (wasAtBottom && logEntries.isNotEmpty()) {
            listState.animateScrollToItem(logEntries.size - 1)
        }
    }

    // Track if user is at the bottom
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index == layoutInfo.totalItemsCount - 1
        }.collect { atBottom ->
            wasAtBottom = atBottom
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.logViewerActivityTitle)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.preferencesDebugLog)) },
                            onClick = {
                                onToggleDebug(!isDebugEnabled)
                            },
                            leadingIcon = {
                                Checkbox(
                                    checked = isDebugEnabled,
                                    onCheckedChange = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.clear_log)) },
                            onClick = {
                                menuExpanded = false
                                onClearClick()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onShareClick) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(R.string.exportConfiguration)
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .horizontalScroll(rememberScrollState())
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = logEntries,
                    key = { "${it.time.time}_${it.hashCode()}" }
                ) { logEntry ->
                    LogEntryRow(
                        logEntry = logEntry,
                        previousEntry = logEntries.getOrNull(logEntries.indexOf(logEntry) - 1)
                    )
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(
    logEntry: LogEntry,
    previousEntry: LogEntry?,
    modifier: Modifier = Modifier
) {
    val isStackTrace = previousEntry?.tag == logEntry.tag && logEntry.message.startsWith("\tat ")

    val displayText = if (isStackTrace) {
        buildAnnotatedString {
            append("    ") // indent
            append(logEntry.message)
        }
    } else {
        buildAnnotatedString {
            // Time part
            append(logEntry.toString().substringBefore(" ${priorityChar(logEntry.priority)}"))

            // Priority and tag part (colored and bold)
            val color = levelToColor(logEntry.priority)
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                append(" ${priorityChar(logEntry.priority)} ${logEntry.tag}:")
            }

            // Message part
            append(" ${logEntry.message}")
        }
    }

    Text(
        text = displayText,
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        maxLines = 1,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

private fun priorityChar(priority: Int): String = when (priority) {
    Log.ASSERT -> "A"
    Log.ERROR -> "E"
    Log.WARN -> "W"
    Log.INFO -> "I"
    Log.DEBUG -> "D"
    Log.VERBOSE -> "V"
    else -> "U"
}

private fun levelToColor(level: Int): Color = when (level) {
    Log.DEBUG -> LogDebugColor
    Log.ERROR -> LogErrorColor
    Log.INFO -> LogInfoColor
    Log.WARN -> LogWarningColor
    else -> LogDefaultColor
}
