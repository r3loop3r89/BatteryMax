package com.example.batterymax.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.batterymax.BatteryMaxApp
import com.example.batterymax.ui.theme.StatusConnected

private data class PermissionStatus(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val permission: String?,
    val granted: Boolean,
    val requiredOnThisDevice: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val preferences = (context.applicationContext as BatteryMaxApp).preferences
    val use24Hour by preferences.use24HourClock.collectAsState()
    var refreshKey by remember { mutableIntStateOf(0) }

    LifecycleResumeEffect(Unit) {
        refreshKey++
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshKey++
    }

    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionName = packageInfo.versionName ?: "—"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }

    val permissions = remember(refreshKey) {
        listOf(
            notificationPermissionStatus(context),
            bluetoothPermissionStatus(context)
        )
    }
    val ignoringBatteryOptimizations = remember(refreshKey) {
        isIgnoringBatteryOptimizations(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SectionHeader("Display")

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIconBadge(
                        icon = Icons.Default.Schedule,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("24-hour time", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (use24Hour) "14:35 format" else "2:35 PM format",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = use24Hour,
                        onCheckedChange = preferences::setUse24HourClock
                    )
                }
            }

            SectionHeader("Background")

            BatteryOptimizationCard(
                unrestricted = ignoringBatteryOptimizations,
                onDisableOptimization = { requestIgnoreBatteryOptimizations(context) },
                onOpenSettings = { openBatteryOptimizationSettings(context) }
            )

            SectionHeader("Permissions")

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                permissions.forEach { status ->
                    PermissionCard(
                        status = status,
                        onRequest = {
                            val permission = status.permission
                            if (permission != null) {
                                permissionLauncher.launch(permission)
                            }
                        },
                        onOpenSettings = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        }
                    )
                }
            }

            SectionHeader("About")

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIconBadge(
                        icon = Icons.Default.BatteryStd,
                        tint = MaterialTheme.colorScheme.primary,
                        size = 48.dp,
                        iconSize = 28.dp
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Battery Max", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Version $versionName · build ${formatBuildStamp(versionCode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Monitors phone and Bluetooth battery levels over time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun StatusIndicator(
    ok: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(
                    if (ok) StatusConnected
                    else MaterialTheme.colorScheme.error
                )
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (ok) StatusConnected else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun BatteryOptimizationCard(
    unrestricted: Boolean,
    onDisableOptimization: () -> Unit,
    onOpenSettings: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsIconBadge(
                    icon = Icons.Default.BatterySaver,
                    tint = if (unrestricted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Battery optimization", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Keep unrestricted so background monitoring is less likely to pause",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            StatusIndicator(
                ok = unrestricted,
                label = if (unrestricted) "Unrestricted" else "Optimized — may pause monitoring"
            )
            if (!unrestricted) {
                Row {
                    TextButton(onClick = onDisableOptimization) {
                        Text("Disable optimization")
                    }
                    TextButton(onClick = onOpenSettings) {
                        Text("Battery settings")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    status: PermissionStatus,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val ok = status.granted || !status.requiredOnThisDevice
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsIconBadge(
                    icon = status.icon,
                    tint = if (ok) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(status.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        status.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            StatusIndicator(ok = ok, label = statusText(status))
            if (status.requiredOnThisDevice && !status.granted) {
                Row {
                    TextButton(onClick = onRequest) {
                        Text("Grant")
                    }
                    TextButton(onClick = onOpenSettings) {
                        Text("App settings")
                    }
                }
            }
        }
    }
}

private fun statusText(status: PermissionStatus): String = when {
    !status.requiredOnThisDevice -> "Not required on this Android version"
    status.granted -> "Granted"
    else -> "Not granted"
}

/** Formats versionCode yyyyMMddHH as yyyy.MMddHH, e.g. 2026070310 → 2026.070310. */
private fun formatBuildStamp(versionCode: Long): String {
    val digits = versionCode.toString()
    return if (digits.length == 10) {
        "${digits.substring(0, 4)}.${digits.substring(4)}"
    } else {
        digits
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        ?: return true
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

@SuppressLint("BatteryLife")
private fun requestIgnoreBatteryOptimizations(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    runCatching { context.startActivity(intent) }
        .onFailure { openBatteryOptimizationSettings(context) }
}

private fun openBatteryOptimizationSettings(context: Context) {
    val intents = listOf(
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    )
    for (intent in intents) {
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            return
        }
    }
}

private fun notificationPermissionStatus(context: Context): PermissionStatus {
    val required = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val granted = if (required) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    return PermissionStatus(
        label = "Notifications",
        description = "Shows the ongoing battery monitoring notification",
        icon = Icons.Default.Notifications,
        permission = if (required) Manifest.permission.POST_NOTIFICATIONS else null,
        granted = granted,
        requiredOnThisDevice = required
    )
}

private fun bluetoothPermissionStatus(context: Context): PermissionStatus {
    val required = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val granted = if (required) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    return PermissionStatus(
        label = "Bluetooth",
        description = "Reads battery level from paired Bluetooth devices",
        icon = Icons.Default.Bluetooth,
        permission = if (required) Manifest.permission.BLUETOOTH_CONNECT else null,
        granted = granted,
        requiredOnThisDevice = required
    )
}