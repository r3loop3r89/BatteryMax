package com.example.batterymax.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.batterymax.BatteryMaxApp
import com.example.batterymax.data.db.BatterySampleEntity
import com.example.batterymax.service.BatteryMonitorService
import com.example.batterymax.util.TimeFormats

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenGraph: (sourceId: String) -> Unit
) {
    val context = LocalContext.current
    val preferences = (context.applicationContext as BatteryMaxApp).preferences
    val use24Hour by preferences.use24HourClock.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val btDevices by viewModel.btDevices.collectAsState()
    val running by viewModel.serviceRunning.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Start even if Bluetooth was denied — phone monitoring uses specialUse FGS.
        BatteryMonitorService.start(context)
    }

    fun startMonitoring() {
        val missing = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        if (missing.isEmpty()) {
            BatteryMonitorService.start(context)
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Background monitoring", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (running) "Recording battery samples" else "Stopped",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = running,
                    onCheckedChange = { enable ->
                        if (enable) startMonitoring() else BatteryMonitorService.stop(context)
                    }
                )
            }
        }

        PhoneBatteryCard(
            sample = phone,
            use24Hour = use24Hour,
            onClick = { onOpenGraph(BatterySampleEntity.SOURCE_PHONE) }
        )

        if (btDevices.isEmpty()) {
            BluetoothBatteryCard(
                deviceName = null,
                sample = null,
                connected = false,
                use24Hour = use24Hour,
                onClick = null
            )
        } else {
            btDevices.forEach { status ->
                BluetoothBatteryCard(
                    deviceName = status.device.name,
                    sample = status.sample,
                    connected = status.connected,
                    use24Hour = use24Hour,
                    onClick = { onOpenGraph(status.device.address) }
                )
            }
        }
    }
}

@Composable
private fun PhoneBatteryCard(
    sample: BatterySampleEntity?,
    use24Hour: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (sample?.isCharging == true) Icons.Default.BatteryChargingFull
                    else Icons.Default.BatteryStd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Phone", style = MaterialTheme.typography.titleMedium)
            }
            if (sample == null) {
                Text(
                    "No data yet — turn on monitoring",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Text(
                    "${sample.levelPercent}%",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    buildString {
                        append(if (sample.isCharging) "Charging" else "Discharging")
                        sample.temperatureC?.let { append(" • $it °C") }
                        sample.voltageMv?.let { append(" • ${it / 1000f} V") }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Updated ${TimeFormats.formatTime(sample.timestamp, use24Hour)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Tap to view history",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun BluetoothBatteryCard(
    deviceName: String?,
    sample: BatterySampleEntity?,
    connected: Boolean,
    use24Hour: Boolean,
    onClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (connected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    tint = if (connected) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    deviceName ?: "Bluetooth device",
                    style = MaterialTheme.typography.titleMedium
                )
                if (deviceName != null && !connected) {
                    Spacer(Modifier.width(8.dp))
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text("Disconnected", style = MaterialTheme.typography.labelSmall)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.BluetoothDisabled,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconContentColor =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            when {
                deviceName == null -> Text(
                    "No device selected — pick one in Devices",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                sample == null -> Text(
                    if (connected) "Waiting for a battery reading…" else "Disconnected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                connected -> {
                    Text(
                        "${sample.levelPercent}%",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        "Updated ${TimeFormats.formatTime(sample.timestamp, use24Hour)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Text(
                        "${sample.levelPercent}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    Text(
                        "Last seen ${TimeFormats.formatTime(sample.timestamp, use24Hour)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onClick != null) {
                Text(
                    "Tap to view history",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
