package com.nihongo.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.BuildConfig
import com.nihongo.app.R
import com.nihongo.app.domain.model.TextSize
import com.nihongo.app.domain.model.ThemeMode
import com.nihongo.app.ui.common.AppViewModelProvider
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.theme.Elevation
import com.nihongo.app.ui.theme.Spacing

private enum class SettingsDialog { THEME, TEXT_SIZE, ACCOUNT, LANGUAGE, NOTIFICATIONS, BACKUP, ABOUT }

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }

    val themeLabel = themeLabel(prefs.themeMode)
    val textSizeLabel = textSizeLabel(prefs.textSize)
    val themeIcon = when (prefs.themeMode) {
        ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
        ThemeMode.LIGHT -> Icons.Filled.LightMode
        ThemeMode.DARK -> Icons.Filled.DarkMode
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.settings_title), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SettingsRow(Icons.Filled.Person, stringResource(R.string.setting_account), stringResource(R.string.setting_account_sub)) {
                dialog = SettingsDialog.ACCOUNT
            }
            SettingsRow(Icons.Filled.Language, stringResource(R.string.setting_language), stringResource(R.string.setting_language_sub)) {
                dialog = SettingsDialog.LANGUAGE
            }
            SettingsRow(themeIcon, stringResource(R.string.setting_theme), themeLabel) {
                dialog = SettingsDialog.THEME
            }
            SettingsRow(Icons.Filled.TextFields, stringResource(R.string.setting_text_size), textSizeLabel) {
                dialog = SettingsDialog.TEXT_SIZE
            }
            SettingsRow(Icons.Filled.Notifications, stringResource(R.string.setting_notifications), stringResource(R.string.setting_notifications_sub)) {
                dialog = SettingsDialog.NOTIFICATIONS
            }
            SettingsRow(Icons.Filled.Cloud, stringResource(R.string.setting_backup), stringResource(R.string.setting_backup_sub)) {
                dialog = SettingsDialog.BACKUP
            }
            SettingsRow(Icons.Filled.Info, stringResource(R.string.setting_about), stringResource(R.string.setting_about_sub, BuildConfig.VERSION_NAME)) {
                dialog = SettingsDialog.ABOUT
            }
            Spacer(Modifier.height(Spacing.sm))
        }
    }

    when (dialog) {
        SettingsDialog.THEME -> ChoiceDialog(
            title = stringResource(R.string.setting_theme),
            options = ThemeMode.entries.map { it to themeLabel(it) },
            selected = prefs.themeMode,
            onSelect = {
                viewModel.setThemeMode(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.TEXT_SIZE -> ChoiceDialog(
            title = stringResource(R.string.setting_text_size),
            options = TextSize.entries.map { it to textSizeLabel(it) },
            selected = prefs.textSize,
            onSelect = {
                viewModel.setTextSize(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.ACCOUNT, SettingsDialog.BACKUP -> InfoDialog(
            title = stringResource(R.string.coming_soon_title),
            message = stringResource(R.string.coming_soon_phase, 8),
            onDismiss = { dialog = null },
        )
        SettingsDialog.NOTIFICATIONS -> InfoDialog(
            title = stringResource(R.string.coming_soon_title),
            message = stringResource(R.string.coming_soon_phase, 9),
            onDismiss = { dialog = null },
        )
        SettingsDialog.LANGUAGE -> InfoDialog(
            title = stringResource(R.string.setting_language),
            message = stringResource(R.string.language_only_id),
            onDismiss = { dialog = null },
        )
        SettingsDialog.ABOUT -> InfoDialog(
            title = stringResource(R.string.setting_about_sub, BuildConfig.VERSION_NAME),
            message = stringResource(R.string.about_body),
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)

@Composable
private fun textSizeLabel(size: TextSize): String = stringResource(
    when (size) {
        TextSize.SMALL -> R.string.text_small
        TextSize.NORMAL -> R.string.text_normal
        TextSize.LARGE -> R.string.text_large
    },
)

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(value) },
                            )
                            .padding(vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Spacer(Modifier.width(Spacing.md))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_ok)) }
        },
    )
}