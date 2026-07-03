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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.batterymax.BatteryMaxApp

private data class PermissionStatus(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val permission: String?,
    val granted: Boolean,
    val requiredOnThisDevice: Boolean
)

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Display", style = MaterialTheme.typography.titleSmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text("24-hour time", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (use24Hour) "Times use 24-hour format (14:35)"
                        else "Times use 12-hour format (2:35 PM)",
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

        Text("About", style = MaterialTheme.typography.titleSmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Battery Max",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    "Version $versionName (${formatBuildStamp(versionCode)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    context.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text("Background", style = MaterialTheme.typography.titleSmall)
        BatteryOptimizationCard(
            unrestricted = ignoringBatteryOptimizations,
            onDisableOptimization = { requestIgnoreBatteryOptimizations(context) },
            onOpenSettings = { openBatteryOptimizationSettings(context) }
        )

        Text("Permissions", style = MaterialTheme.typography.titleSmall)
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
}

@Composable
private fun BatteryOptimizationCard(
    unrestricted: Boolean,
    onDisableOptimization: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.BatterySaver,
                    contentDescription = null,
                    tint = if (unrestricted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                    Text("Battery optimization", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Turn this off so monitoring is less likely to be stopped in the background",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (unrestricted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (unrestricted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Text(
                if (unrestricted) "Unrestricted (recommended)" else "Optimized (may pause monitoring)",
                style = MaterialTheme.typography.bodyMedium,
                color = if (unrestricted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.padding(top = 8.dp)
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    status.icon,
                    contentDescription = null,
                    tint = if (status.granted || !status.requiredOnThisDevice) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                    Text(status.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        status.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (status.granted || !status.requiredOnThisDevice) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = if (status.granted || !status.requiredOnThisDevice) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Text(
                statusText(status),
                style = MaterialTheme.typography.bodyMedium,
                color = if (status.granted || !status.requiredOnThisDevice) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.padding(top = 8.dp)
            )
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
