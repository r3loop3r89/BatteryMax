package com.example.batterymax.ui.devices

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DevicesScreen(viewModel: DevicesViewModel) {
    var permissionGranted by remember { mutableStateOf(viewModel.hasBluetoothPermission()) }
    val bondedDevices by viewModel.bondedDevices.collectAsState()
    val tracked by viewModel.trackedDevice.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) viewModel.refreshBondedDevices()
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) viewModel.refreshBondedDevices()
    }

    if (!permissionGranted) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Bluetooth permission is required to list paired devices",
                    style = MaterialTheme.typography.bodyLarge
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
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        tracked?.let { device ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tracking", style = MaterialTheme.typography.labelMedium)
                    Text(device.name, style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = viewModel::clearDevice) {
                        Text("Stop tracking")
                    }
                }
            }
        }

        Text(
            "Paired devices",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (bondedDevices.isEmpty()) {
            Text(
                "No paired Bluetooth devices found. Pair a device in system settings first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn {
                items(bondedDevices, key = { it.address }) { device ->
                    val isTracked = tracked?.address == device.address
                    ListItem(
                        headlineContent = { Text(device.name) },
                        supportingContent = { Text(device.address) },
                        leadingContent = {
                            Icon(Icons.Default.Bluetooth, contentDescription = null)
                        },
                        trailingContent = {
                            if (isTracked) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Tracked",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                TextButton(onClick = { viewModel.selectDevice(device) }) {
                                    Text("Track")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
