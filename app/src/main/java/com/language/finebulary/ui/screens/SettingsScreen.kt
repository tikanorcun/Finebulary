package com.language.finebulary.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.language.finebulary.R
import com.language.finebulary.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    onNavigateToApiKeys: () -> Unit,
    onNavigateToDangerZone: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    
    val appLanguage by viewModel.appLanguage.collectAsState()
    val autoCloudBackup by viewModel.autoCloudBackup.collectAsState()

    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val startHour by viewModel.reminderStartHour.collectAsState()
    val endHour by viewModel.reminderEndHour.collectAsState()
    val dailyNotificationCount by viewModel.dailyNotificationCount.collectAsState()
    val reminderCourseIds by viewModel.reminderCourseIds.collectAsState()
    val personalFolders by viewModel.personalFolders.collectAsState()
    val themeColor by viewModel.appThemeColor.collectAsState()
    val currentTheme by viewModel.themeMode.collectAsState()

    var showTimePicker by remember { mutableStateOf(value = false) }
    var showNotificationSettings by remember { mutableStateOf(false) }
    var isSettingStartHour by remember { mutableStateOf(true) }
    
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = if (isSettingStartHour) startHour else endHour,
            initialMinute = 0
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = { 
                        if (isSettingStartHour) {
                            viewModel.setReminderTimeRange(timePickerState.hour, 0, endHour, 0)
                        } else {
                            viewModel.setReminderTimeRange(startHour, 0, timePickerState.hour, 0)
                        }
                        showTimePicker = false 
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.select_time), 
                        style = MaterialTheme.typography.titleMedium, 
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    var showAppLangMenu by remember { mutableStateOf(false) }
    val dataRestoredMsg = stringResource(R.string.data_restored)
    val saveSuccessMsg = stringResource(R.string.backup_saved_success)
    val saveErrorMsg = stringResource(R.string.backup_saved_error)

    val exportLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            viewModel.viewModelScope.launch {
                try {
                    val json = viewModel.exportData(includeVocabulary = true, includeSettings = true)
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        out.write(json.toByteArray())
                    }
                    android.widget.Toast.makeText(context, saveSuccessMsg, android.widget.Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    android.widget.Toast.makeText(context, saveErrorMsg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            if (content != null) { 
                viewModel.importData(content)
                android.widget.Toast.makeText(context, dataRestoredMsg, android.widget.Toast.LENGTH_SHORT).show() 
            }
        }
    }

    if (showNotificationSettings) {
        ModalBottomSheet(onDismissRequest = { showNotificationSettings = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.notification_settings), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                Text(stringResource(R.string.active_hours), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { isSettingStartHour = true; showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.reminder_start, startHour))
                    }
                    OutlinedButton(
                        onClick = { isSettingStartHour = false; showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.reminder_end, endHour))
                    }
                }

                Spacer(Modifier.height(8.dp))
                
                Text(stringResource(R.string.daily_notification_count_label, dailyNotificationCount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Slider(
                    value = dailyNotificationCount.toFloat(),
                    onValueChange = { viewModel.setDailyNotificationCount(it.toInt()) },
                    valueRange = 1f..24f,
                    steps = 22
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(stringResource(R.string.included_folders), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (personalFolders.isEmpty()) {
                    Text(stringResource(R.string.no_word_folders), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                } else {
                    Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        personalFolders.forEach { folderItem ->
                            val courseIdStr = folderItem.course.id.toString()
                            val isChecked = reminderCourseIds.isEmpty() || reminderCourseIds.contains(courseIdStr)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val currentSet = reminderCourseIds.toMutableSet()
                                        if (reminderCourseIds.isEmpty()) {
                                            personalFolders.forEach { currentSet.add(it.course.id.toString()) }
                                        }
                                        if (currentSet.contains(courseIdStr)) {
                                            currentSet.remove(courseIdStr)
                                        } else {
                                            currentSet.add(courseIdStr)
                                        }
                                        viewModel.setReminderCourseIds(currentSet)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        val currentSet = reminderCourseIds.toMutableSet()
                                        if (reminderCourseIds.isEmpty()) {
                                            personalFolders.forEach { currentSet.add(it.course.id.toString()) }
                                        }
                                        if (checked) {
                                            currentSet.add(courseIdStr)
                                        } else {
                                            currentSet.remove(courseIdStr)
                                        }
                                        viewModel.setReminderCourseIds(currentSet)
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(folderItem.course.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Compact Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.settings_hub),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Connectivity Hub Item
            SettingsHubItem(
                title = stringResource(R.string.connectivity_ai),
                subtitle = stringResource(R.string.connectivity_ai_desc),
                icon = Icons.Default.CloudQueue,
                onClick = onNavigateToApiKeys
            )

            HorizontalDivider()

            // Sensory Section
            SettingsSection(title = stringResource(R.string.sensory_sound), icon = Icons.AutoMirrored.Filled.VolumeUp) {
                SettingsToggle(stringResource(R.string.sound_effects), stringResource(R.string.sound_effects_desc), soundEnabled) { viewModel.setSoundEnabled(it) }
                SettingsToggle(stringResource(R.string.haptic_feedback), stringResource(R.string.haptic_feedback_desc), hapticEnabled) { viewModel.setHapticEnabled(it) }
            }

            // Engagement & Habit Section
            SettingsSection(title = stringResource(R.string.engagement_habit), icon = Icons.Default.EventRepeat) {
                SettingsToggle(
                    label = stringResource(R.string.reminder_label),
                    subtitle = stringResource(R.string.reminder_desc),
                    checked = remindersEnabled,
                ) { viewModel.setRemindersEnabled(it) }
                
                if (remindersEnabled) {
                    OutlinedButton(
                        onClick = { showNotificationSettings = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.advanced_notification_settings))
                    }
                }
            }

            // Interface Section
            SettingsSection(title = stringResource(R.string.app_interface), icon = Icons.Default.Language) {
                // Theme Mode & Color combined into a cleaner layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleSmall)
                        var showModeMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedCard(
                                onClick = { showModeMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val modeIcon = when (currentTheme) {
                                        com.language.finebulary.data.pref.ThemeMode.AUTO -> Icons.Default.BrightnessAuto
                                        com.language.finebulary.data.pref.ThemeMode.LIGHT -> Icons.Default.LightMode
                                        com.language.finebulary.data.pref.ThemeMode.NIGHT -> Icons.Default.DarkMode
                                    }
                                    val modeLabel = when (currentTheme) {
                                        com.language.finebulary.data.pref.ThemeMode.AUTO -> stringResource(R.string.theme_auto)
                                        com.language.finebulary.data.pref.ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                        com.language.finebulary.data.pref.ThemeMode.NIGHT -> stringResource(R.string.theme_night)
                                    }
                                    Icon(modeIcon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(modeLabel, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                            DropdownMenu(expanded = showModeMenu, onDismissRequest = { showModeMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.theme_auto)) },
                                    leadingIcon = { Icon(Icons.Default.BrightnessAuto, null) },
                                    onClick = { viewModel.setThemeMode(com.language.finebulary.data.pref.ThemeMode.AUTO); showModeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.theme_light)) },
                                    leadingIcon = { Icon(Icons.Default.LightMode, null) },
                                    onClick = { viewModel.setThemeMode(com.language.finebulary.data.pref.ThemeMode.LIGHT); showModeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.theme_night)) },
                                    leadingIcon = { Icon(Icons.Default.DarkMode, null) },
                                    onClick = { viewModel.setThemeMode(com.language.finebulary.data.pref.ThemeMode.NIGHT); showModeMenu = false }
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.theme_color_label), style = MaterialTheme.typography.titleSmall)
                        var showColorMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedCard(
                                onClick = { showColorMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val colorHex = when (themeColor) {
                                        com.language.finebulary.data.pref.AppThemeColor.WARM -> Color(0xFFFF7043)
                                        com.language.finebulary.data.pref.AppThemeColor.OCEAN -> Color(0xFF0288D1)
                                        com.language.finebulary.data.pref.AppThemeColor.FOREST -> Color(0xFF388E3C)
                                        com.language.finebulary.data.pref.AppThemeColor.NIGHT_PLUS -> Color.Black
                                        com.language.finebulary.data.pref.AppThemeColor.SYSTEM -> Color(0xFF6200EE)
                                    }
                                    val colorLabel = when (themeColor) {
                                        com.language.finebulary.data.pref.AppThemeColor.WARM -> stringResource(R.string.color_warm)
                                        com.language.finebulary.data.pref.AppThemeColor.OCEAN -> stringResource(R.string.color_ocean)
                                        com.language.finebulary.data.pref.AppThemeColor.FOREST -> stringResource(R.string.color_forest)
                                        com.language.finebulary.data.pref.AppThemeColor.NIGHT_PLUS -> stringResource(R.string.color_night_plus)
                                        com.language.finebulary.data.pref.AppThemeColor.SYSTEM -> stringResource(R.string.color_system)
                                    }
                                    Box(Modifier.size(14.dp).background(colorHex, CircleShape))
                                    Spacer(Modifier.width(8.dp))
                                    Text(colorLabel, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                            DropdownMenu(expanded = showColorMenu, onDismissRequest = { showColorMenu = false }) {
                                com.language.finebulary.data.pref.AppThemeColor.entries.forEach { color ->
                                    val hex = when (color) {
                                        com.language.finebulary.data.pref.AppThemeColor.WARM -> Color(0xFFFF7043)
                                        com.language.finebulary.data.pref.AppThemeColor.OCEAN -> Color(0xFF0288D1)
                                        com.language.finebulary.data.pref.AppThemeColor.FOREST -> Color(0xFF388E3C)
                                        com.language.finebulary.data.pref.AppThemeColor.NIGHT_PLUS -> Color.Black
                                        com.language.finebulary.data.pref.AppThemeColor.SYSTEM -> Color(0xFF6200EE)
                                    }
                                    val label = when (color) {
                                        com.language.finebulary.data.pref.AppThemeColor.WARM -> stringResource(R.string.color_warm)
                                        com.language.finebulary.data.pref.AppThemeColor.OCEAN -> stringResource(R.string.color_ocean)
                                        com.language.finebulary.data.pref.AppThemeColor.FOREST -> stringResource(R.string.color_forest)
                                        com.language.finebulary.data.pref.AppThemeColor.NIGHT_PLUS -> stringResource(R.string.color_night_plus)
                                        com.language.finebulary.data.pref.AppThemeColor.SYSTEM -> stringResource(R.string.color_system)
                                    }
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        leadingIcon = { Box(Modifier.size(12.dp).background(hex, CircleShape)) },
                                        onClick = { viewModel.setAppThemeColor(color); showColorMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                
                Text(stringResource(R.string.native_lang_label), style = MaterialTheme.typography.titleSmall)
                Box {
                    OutlinedTextField(
                        value = if (appLanguage == "tr") "Türkçe" else "English",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().clickable { showAppLangMenu = true },
                        enabled = false,
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        shape = MaterialTheme.shapes.small
                    )
                    DropdownMenu(expanded = showAppLangMenu, onDismissRequest = { showAppLangMenu = false }) {
                        DropdownMenuItem(text = { Text("English") }, onClick = { viewModel.setAppLanguage("en"); showAppLangMenu = false })
                        DropdownMenuItem(text = { Text("Türkçe") }, onClick = { viewModel.setAppLanguage("tr"); showAppLangMenu = false })
                    }
                }
            }

            // Data & Backup Grid
            SettingsSection(title = stringResource(R.string.data_backup), icon = Icons.Default.Storage) {
                SettingsToggle(
                    label = stringResource(R.string.auto_cloud_backup),
                    subtitle = stringResource(R.string.auto_cloud_backup_desc),
                    checked = autoCloudBackup,
                    onCheckedChange = { viewModel.setAutoCloudBackup(it) }
                )

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BackupGridItem(
                        title = stringResource(R.string.export_backup),
                        icon = Icons.Default.CloudUpload,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                            exportLauncher.launch("finebulary_backup_$timestamp.json")
                        }
                    )
                    BackupGridItem(
                        title = stringResource(R.string.restore_backup),
                        icon = Icons.Default.CloudDownload,
                        modifier = Modifier.weight(1f),
                        onClick = { importLauncher.launch("application/json") }
                    )
                }
            }

            // Diğer İzinler (Overlay)
            SettingsSection(title = stringResource(R.string.other_permissions), icon = Icons.Default.Layers) {
                val overlayPermissionGranted = Settings.canDrawOverlays(context)
                SettingsToggle(
                    stringResource(R.string.overlay_permission_title),
                    stringResource(R.string.overlay_permission_desc),
                    overlayPermissionGranted
                ) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            }

            // Danger Zone Link
            SettingsHubItem(
                title = stringResource(R.string.danger_zone),
                subtitle = stringResource(R.string.danger_zone_desc),
                icon = Icons.Default.WarningAmber,
                color = MaterialTheme.colorScheme.error,
                onClick = onNavigateToDangerZone
            )

            // About App Link
            SettingsHubItem(
                title = stringResource(R.string.about_app),
                subtitle = stringResource(R.string.about_desc),
                icon = Icons.Default.Info,
                onClick = onNavigateToAbout
            )
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsHubItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun BackupGridItem(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsSection(
    title: String, 
    icon: ImageVector, 
    color: Color = MaterialTheme.colorScheme.primary, 
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(12.dp))
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
        }
    }
}

@Composable
fun SettingsToggle(label: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
