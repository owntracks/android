package org.owntracks.android.ui.preferences.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.owntracks.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    effectiveConfiguration: String,
    preferenceKeys: List<String>,
    onBackClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportFileClick: () -> Unit,
    onSetPreferenceValue: (key: String, value: String) -> Result<Unit>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditorDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.configurationManagement)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.exportConfiguration)) },
                                onClick = {
                                    showMenu = false
                                    onExportClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.importConfig)) },
                                onClick = {
                                    showMenu = false
                                    onImportFileClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.preferencesEditor)) },
                                onClick = {
                                    showMenu = false
                                    showEditorDialog = true
                                }
                            )
                        }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        SelectionContainer {
            Text(
                text = effectiveConfiguration,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp)
            )
        }
    }

    if (showEditorDialog) {
        PreferenceEditorDialog(
            preferenceKeys = preferenceKeys,
            onDismiss = { showEditorDialog = false },
            onConfirm = { key, value ->
                val result = onSetPreferenceValue(key, value)
                if (result.isSuccess) {
                    showEditorDialog = false
                }
                result
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferenceEditorDialog(
    preferenceKeys: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (key: String, value: String) -> Result<Unit>
) {
    var selectedKey by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var keyError by remember { mutableStateOf<String?>(null) }
    var valueError by remember { mutableStateOf<String?>(null) }

    val filteredKeys = remember(selectedKey, preferenceKeys) {
        if (selectedKey.isBlank()) {
            preferenceKeys
        } else {
            preferenceKeys.filter { it.contains(selectedKey, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.preferencesEditor)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.preferencesEditorDescription),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedKey,
                        onValueChange = {
                            selectedKey = it
                            keyError = null
                            expanded = true
                        },
                        label = { Text(stringResource(R.string.preferencesEditorKey)) },
                        isError = keyError != null,
                        supportingText = keyError?.let { { Text(it) } },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                        singleLine = true
                    )

                    if (filteredKeys.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            filteredKeys.take(10).forEach { key ->
                                DropdownMenuItem(
                                    text = { Text(key) },
                                    onClick = {
                                        selectedKey = key
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        valueError = null
                    },
                    label = { Text(stringResource(R.string.preferencesEditorValue)) },
                    isError = valueError != null,
                    supportingText = valueError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = onConfirm(selectedKey, value)
                    result.exceptionOrNull()?.let { error ->
                        when (error) {
                            is NoSuchElementException -> keyError = error.message ?: "Invalid key"
                            is IllegalArgumentException -> valueError = error.message ?: "Invalid value"
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
