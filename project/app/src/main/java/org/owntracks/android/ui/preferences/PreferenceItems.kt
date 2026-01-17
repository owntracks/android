package org.owntracks.android.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.owntracks.android.R

/**
 * A category header for grouping related preferences
 */
@Composable
fun PreferenceCategory(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

/**
 * Base clickable preference item with optional icon
 */
@Composable
fun PreferenceItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: Painter? = null,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                },
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
                )
            }
        }

        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * Switch preference for boolean values
 */
@Composable
fun SwitchPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: Painter? = null,
    enabled: Boolean = true
) {
    PreferenceItem(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        onClick = { if (enabled) onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = { if (enabled) onCheckedChange(it) },
                enabled = enabled
            )
        },
        modifier = modifier
    )
}

/**
 * Text input preference with dialog
 */
@Composable
fun EditTextPreference(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: Painter? = null,
    enabled: Boolean = true,
    dialogMessage: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    validator: ((String) -> Boolean)? = null,
    validationError: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    var editedValue by remember(value) { mutableStateOf(value) }
    var isError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val displaySummary = if (isPassword && value.isNotBlank()) {
        stringResource(R.string.preferencesSet)
    } else if (value.isNotBlank()) {
        value
    } else {
        stringResource(R.string.preferencesNotSet)
    }

    PreferenceItem(
        title = title,
        summary = displaySummary,
        icon = icon,
        enabled = enabled,
        onClick = {
            editedValue = value
            isError = false
            passwordVisible = false
            showDialog = true
        },
        modifier = modifier
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dialogMessage != null) {
                        Text(
                            text = dialogMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    OutlinedTextField(
                        value = editedValue,
                        onValueChange = {
                            editedValue = it
                            isError = validator?.invoke(it) == false
                        },
                        isError = isError,
                        supportingText = if (isError && validationError != null) {
                            { Text(validationError) }
                        } else null,
                        visualTransformation = if (isPassword && !passwordVisible) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        trailingIcon = if (isPassword) {
                            {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Filled.VisibilityOff
                                        } else {
                                            Icons.Filled.Visibility
                                        },
                                        contentDescription = if (passwordVisible) {
                                            stringResource(R.string.hide_password)
                                        } else {
                                            stringResource(R.string.show_password)
                                        }
                                    )
                                }
                            }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (validator == null || validator(editedValue)) {
                            onValueChange(editedValue)
                            showDialog = false
                        } else {
                            isError = true
                        }
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

/**
 * Integer input preference with dialog
 */
@Composable
fun EditIntPreference(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: Painter? = null,
    enabled: Boolean = true,
    dialogMessage: String? = null,
    minValue: Int? = null,
    maxValue: Int? = null,
    validationError: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    var editedValue by remember(value) { mutableStateOf(value.toString()) }
    var isError by remember { mutableStateOf(false) }

    val displaySummary = summary ?: value.toString()

    PreferenceItem(
        title = title,
        summary = displaySummary,
        icon = icon,
        enabled = enabled,
        onClick = {
            editedValue = value.toString()
            isError = false
            showDialog = true
        },
        modifier = modifier
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dialogMessage != null) {
                        Text(
                            text = dialogMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    OutlinedTextField(
                        value = editedValue,
                        onValueChange = { newValue ->
                            editedValue = newValue.filter { it.isDigit() }
                            val intValue = editedValue.toIntOrNull()
                            isError = intValue == null ||
                                    (minValue != null && intValue < minValue) ||
                                    (maxValue != null && intValue > maxValue)
                        },
                        isError = isError,
                        supportingText = if (isError && validationError != null) {
                            { Text(validationError) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intValue = editedValue.toIntOrNull()
                        if (intValue != null &&
                            (minValue == null || intValue >= minValue) &&
                            (maxValue == null || intValue <= maxValue)
                        ) {
                            onValueChange(intValue)
                            showDialog = false
                        } else {
                            isError = true
                        }
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

/**
 * List preference with radio button dialog
 */
@Composable
fun <T> ListPreference(
    title: String,
    value: T,
    entries: List<Pair<T, String>>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: Painter? = null,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    val displaySummary = summary ?: entries.find { it.first == value }?.second ?: value.toString()

    PreferenceItem(
        title = title,
        summary = displaySummary,
        icon = icon,
        enabled = enabled,
        onClick = { showDialog = true },
        modifier = modifier
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    entries.forEach { (entryValue, entryLabel) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(entryValue)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = entryValue == value,
                                onClick = {
                                    onValueChange(entryValue)
                                    showDialog = false
                                }
                            )
                            Text(
                                text = entryLabel,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

/**
 * Navigation preference that shows an arrow indicator
 */
@Composable
fun NavigationPreference(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: Painter? = null,
    enabled: Boolean = true
) {
    PreferenceItem(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Info preference that just displays information (not clickable)
 */
@Composable
fun InfoPreference(
    title: String? = null,
    summary: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Divider for separating preference groups
 */
@Composable
fun PreferenceDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
