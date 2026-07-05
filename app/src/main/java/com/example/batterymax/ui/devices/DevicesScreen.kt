package com.example.batterymax.ui.devices

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.batterymax.ui.theme.StatusConnected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(viewModel: DevicesViewModel) {
    var permissionGranted by remember { mutableStateOf(viewModel.hasBluetoothPermission()) }
    val bondedDevices by viewModel.bondedDevices.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val trackedDevices by viewModel.trackedDevices.collectAsState()
    val trackedAddresses = remember(trackedDevices) {
        trackedDevices.map { it.address }.toSet()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) viewModel.refreshBondedDevices()
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) viewModel.refreshBondedDevices()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Devices") })
        }
    ) { innerPadding ->
        if (!permissionGranted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DeviceIconBadge(
                            icon = Icons.Default.Bluetooth,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 48.dp,
                            iconSize = 28.dp
                        )
                        Text(
                            "Bluetooth permission required",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Grant access to list paired devices and read their battery levels",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            } else {
                                permissionGranted = true
                            }
                        }) {
                            Text("Grant permission")
                        }
                    }
                }
            }
            return@Scaffold
        }

        val connectedDevices = remember(bondedDevices) { bondedDevices.filter { it.connected } }
        val otherDevices = remember(bondedDevices) { bondedDevices.filterNot { it.connected } }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refreshBondedDevices,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (trackedDevices.isNotEmpty()) {
                    item(key = "tracking_header") {
                        SectionHeader(
                            title = "Tracking",
                            subtitle = "${trackedDevices.size} device${if (trackedDevices.size == 1) "" else "s"}"
                        )
                    }
                    items(trackedDevices, key = { "tracked_${it.address}" }) { device ->
                        TrackedDeviceCard(
                            name = device.name,
                            address = device.address,
                            onStop = { viewModel.removeDevice(device.address) }
                        )
                    }
                    item(key = "tracking_spacer") {
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (bondedDevices.isEmpty()) {
                    item(key = "empty") {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DeviceIconBadge(
                                    icon = Icons.Default.Bluetooth,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("No paired devices", style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Pair a Bluetooth device in system settings, then pull to refresh",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (connectedDevices.isNotEmpty()) {
                        item(key = "connected_header") {
                            SectionHeader(
                                title = "Connected",
                                subtitle = "${connectedDevices.size} online"
                            )
                        }
                        items(connectedDevices, key = { "connected_${it.address}" }) { device ->
                            DeviceCard(
                                device = device,
                                isTracked = device.address in trackedAddresses,
                                onTrack = { viewModel.selectDevice(device) },
                                onStop = { viewModel.removeDevice(device.address) }
                            )
                        }
                    }

                    item(key = "paired_header") {
                        SectionHeader(
                            title = if (connectedDevices.isEmpty()) {
                                "Paired devices"
                            } else {
                                "Other paired devices"
                            },
                            subtitle = if (otherDevices.isEmpty()) null else "${otherDevices.size} offline"
                        )
                    }

                    if (otherDevices.isEmpty()) {
                        item(key = "no_other") {
                            Text(
                                "No other paired devices",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    } else {
                        items(otherDevices, key = { "other_${it.address}" }) { device ->
                            DeviceCard(
                                device = device,
                                isTracked = device.address in trackedAddresses,
                                onTrack = { viewModel.selectDevice(device) },
                                onStop = { viewModel.removeDevice(device.address) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceIconBadge(
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
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun ConnectionStatus(connected: Boolean, modifier: Modifier = Modifier) {
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
                    if (connected) StatusConnected
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
        )
        Text(
            if (connected) "Connected" else "Offline",
            style = MaterialTheme.typography.labelSmall,
            color = if (connected) {
                StatusConnected
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun TrackedDeviceCard(
    name: String,
    address: String,
    onStop: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeviceIconBadge(
                icon = Icons.Default.BluetoothConnected,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    address,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tracking",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusConnected
                )
            }
            TextButton(onClick = onStop) {
                Text("Stop")
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: BondedDevice,
    isTracked: Boolean,
    onTrack: () -> Unit,
    onStop: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeviceIconBadge(
                icon = if (device.connected) {
                    Icons.Default.BluetoothConnected
                } else {
                    Icons.Default.Bluetooth
                },
                tint = if (device.connected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    device.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                ConnectionStatus(connected = device.connected)
            }
            if (isTracked) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Tracking",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusConnected
                    )
                    TextButton(onClick = onStop) {
                        Text("Stop")
                    }
                }
            } else {
                TextButton(onClick = onTrack) {
                    Text("Track")
                }
            }
        }
    }
}