package com.example.batterymax.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.batterymax.BatteryMaxApp
import com.example.batterymax.data.db.BatterySampleEntity
import com.example.batterymax.service.BatteryMonitorService
import com.example.batterymax.ui.theme.StatusConnected
import com.example.batterymax.ui.theme.batteryLevelColor
import com.example.batterymax.util.TimeFormats

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BatteryMax", style = MaterialTheme.typography.titleLarge)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            MonitoringDot(active = running)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (running) "Recording battery samples" else "Monitoring stopped",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Switch(
                        checked = running,
                        onCheckedChange = { enable ->
                            if (enable) startMonitoring() else BatteryMonitorService.stop(context)
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
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
            SectionHeader("This phone")

            PhoneHeroCard(
                sample = phone,
                use24Hour = use24Hour,
                onClick = { onOpenGraph(BatterySampleEntity.SOURCE_PHONE) }
            )

            SectionHeader(
                title = "Bluetooth devices",
                subtitle = when (btDevices.size) {
                    0 -> null
                    1 -> "1 tracked"
                    else -> "${btDevices.size} tracked"
                }
            )

            when {
                btDevices.isEmpty() -> BluetoothEmptyCard()
                btDevices.size == 1 -> {
                    val status = btDevices.first()
                    BluetoothCompactCard(
                        deviceName = status.device.name,
                        sample = status.sample,
                        connected = status.connected,
                        use24Hour = use24Hour,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenGraph(status.device.address) }
                    )
                }
                else -> {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        btDevices.forEach { status ->
                            BluetoothCompactCard(
                                deviceName = status.device.name,
                                sample = status.sample,
                                connected = status.connected,
                                use24Hour = use24Hour,
                                modifier = Modifier.width(156.dp),
                                onClick = { onOpenGraph(status.device.address) }
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
        modifier = Modifier.fillMaxWidth(),
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
private fun MonitoringDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(
                if (active) StatusConnected
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
    )
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
private fun BatteryRing(
    levelPercent: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 10.dp,
    isCharging: Boolean = false,
    dimmed: Boolean = false
) {
    val levelColor = batteryLevelColor(levelPercent, dimmed)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val targetSweep = (levelPercent ?: 0) / 100f
    val animatedSweep by animateFloatAsState(
        targetValue = targetSweep,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "batterySweep"
    )
    val chargingPulse = if (isCharging) {
        val transition = rememberInfiniteTransition(label = "chargingPulse")
        transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "chargingAlpha"
        ).value
    } else {
        1f
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2f
            val arcSize = this.size.width - stroke.width

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                style = stroke
            )
            if (levelPercent != null && animatedSweep > 0f) {
                drawArc(
                    color = levelColor.copy(alpha = chargingPulse),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedSweep,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                    style = stroke
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (levelPercent != null) {
                Text(
                    "$levelPercent",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (size >= 100.dp) 36.sp else 22.sp
                    ),
                    color = levelColor
                )
                Text(
                    "%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "—",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isCharging) {
                Icon(
                    Icons.Default.BatteryChargingFull,
                    contentDescription = "Charging",
                    tint = levelColor,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(if (size >= 100.dp) 20.dp else 16.dp)
                )
            }
        }
    }
}

@Composable
private fun PhoneHeroCard(
    sample: BatterySampleEntity?,
    use24Hour: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BatteryRing(
                levelPercent = sample?.levelPercent,
                isCharging = sample?.isCharging == true,
                size = 128.dp,
                strokeWidth = 11.dp
            )
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Phone", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                if (sample == null) {
                    Text(
                        "No data yet — turn on monitoring",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        buildString {
                            append(if (sample.isCharging) "Charging" else "Discharging")
                            sample.temperatureC?.let { append(" · $it °C") }
                            sample.voltageMv?.let { append(" · ${it / 1000f} V") }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Updated ${TimeFormats.formatTime(sample.timestamp, use24Hour)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View history",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BluetoothCompactCard(
    deviceName: String,
    sample: BatterySampleEntity?,
    connected: Boolean,
    use24Hour: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BatteryRing(
                levelPercent = sample?.levelPercent,
                isCharging = false,
                dimmed = !connected,
                size = 72.dp,
                strokeWidth = 7.dp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                deviceName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            ConnectionStatus(connected = connected)
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    sample == null && connected -> "Waiting for reading…"
                    sample == null -> "No reading"
                    connected -> "Updated ${TimeFormats.formatTime(sample.timestamp, use24Hour)}"
                    else -> "Last seen ${TimeFormats.formatTime(sample.timestamp, use24Hour)}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BluetoothEmptyCard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("No devices tracked", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pick paired Bluetooth devices in the Devices tab",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}