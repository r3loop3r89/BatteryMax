package com.example.batterymax.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.batterymax.BatteryMaxApp
import com.example.batterymax.MainActivity
import com.example.batterymax.R
import com.example.batterymax.bluetooth.BtBatteryEvent
import com.example.batterymax.bluetooth.BtBatteryReader
import com.example.batterymax.data.BatteryRepository
import com.example.batterymax.data.db.BatterySampleEntity
import com.example.batterymax.data.db.TrackedDeviceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that samples the phone battery and any tracked Bluetooth
 * devices' batteries, persisting readings through [BatteryRepository].
 */
class BatteryMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: BatteryRepository
    private lateinit var btReader: BtBatteryReader

    private var lastPhone: BatterySampleEntity? = null
    private val lastBtLevels = mutableMapOf<String, Int>()
    private val btConnectedMap = mutableMapOf<String, Boolean>()
    private var trackedDevices: List<TrackedDeviceEntity> = emptyList()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                handlePhoneBattery(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = (application as BatteryMaxApp).repository
        btReader = BtBatteryReader(this)
        isRunning.value = true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()

        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )

        // Watch every tracked Bluetooth device; restart watchers when the list changes.
        scope.launch {
            repository.trackedDevices.flatMapLatest { devices ->
                trackedDevices = devices
                val activeAddresses = devices.map { it.address }.toSet()
                lastBtLevels.keys.retainAll(activeAddresses)
                btConnectedMap.keys.retainAll(activeAddresses)
                publishBtConnectionStates()
                updateNotification()

                if (devices.isEmpty()) {
                    emptyFlow()
                } else {
                    devices.map { device ->
                        btReader.watch(device.address).map { event -> device.address to event }
                    }.merge()
                }
            }.collect { (address, event) ->
                when (event) {
                    is BtBatteryEvent.Battery -> {
                        lastBtLevels[address] = event.levelPercent
                        repository.recordSample(
                            BatterySampleEntity(
                                timestamp = System.currentTimeMillis(),
                                levelPercent = event.levelPercent,
                                isCharging = false,
                                source = address
                            )
                        )
                    }
                    is BtBatteryEvent.Connection -> {
                        btConnectedMap[address] = event.connected
                        publishBtConnectionStates()
                    }
                }
                updateNotification()
            }
        }

        scope.launch {
            var ticks = 0
            while (isActive) {
                delay(BatteryRepository.MIN_SAMPLE_INTERVAL_MS)
                lastPhone?.let {
                    repository.recordSample(it.copy(id = 0, timestamp = System.currentTimeMillis()))
                }
                lastBtLevels.forEach { (address, level) ->
                    repository.recordSample(
                        BatterySampleEntity(
                            timestamp = System.currentTimeMillis(),
                            levelPercent = level,
                            isCharging = false,
                            source = address
                        )
                    )
                }
                if (++ticks % PRUNE_EVERY_TICKS == 0) repository.pruneOldSamples()
            }
        }

        return START_STICKY
    }

    private fun handlePhoneBattery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return
        val percent = (level * 100) / scale
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, Int.MIN_VALUE)

        val sample = BatterySampleEntity(
            timestamp = System.currentTimeMillis(),
            levelPercent = percent,
            isCharging = charging,
            temperatureC = if (tempTenths != Int.MIN_VALUE) tempTenths / 10f else null,
            voltageMv = if (voltage != Int.MIN_VALUE) voltage else null,
            source = BatterySampleEntity.SOURCE_PHONE
        )
        lastPhone = sample
        scope.launch { repository.recordSample(sample) }
        updateNotification()
    }

    private fun startAsForeground() {
        createChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (hasBluetoothConnectPermission()) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    private fun buildNotification(): Notification {
        val phoneText = lastPhone?.let { "Phone ${it.levelPercent}%" } ?: "Phone —"
        val btLines = trackedDevices.map { device ->
            val level = lastBtLevels[device.address]?.let { "$it%" }
                ?: if (btConnectedMap[device.address] == true) "…" else "off"
            "${device.name} $level"
        }
        // Collapsed: one-line summary. Expanded: one device per line.
        val collapsedText = buildString {
            append(phoneText)
            if (btLines.isNotEmpty()) {
                append(" • ")
                append(btLines.joinToString(" • "))
            }
        }
        val expandedText = (listOf(phoneText) + btLines).joinToString("\n")

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_foreground)
            .setContentTitle("Battery monitoring active")
            .setContentText(collapsedText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Battery monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Persistent status while battery monitoring is running"
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun publishBtConnectionStates() {
        btConnectionStates.value = btConnectedMap.toMap()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(batteryReceiver) }
        scope.cancel()
        isRunning.value = false
        btConnectionStates.value = emptyMap()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "battery_monitor"
        private const val NOTIFICATION_ID = 1
        private const val PRUNE_EVERY_TICKS = 12
        private const val PREFS = "battery_monitor"
        private const val KEY_ENABLED = "monitoring_enabled"

        private val isRunning = MutableStateFlow(false)
        val running: StateFlow<Boolean> get() = isRunning

        /** Address → connected for each tracked Bluetooth device. */
        private val btConnectionStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
        val connectionStates: StateFlow<Map<String, Boolean>> get() = btConnectionStates

        fun start(context: Context) {
            setEnabled(context, true)
            val intent = Intent(context, BatteryMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            setEnabled(context, false)
            context.stopService(Intent(context, BatteryMonitorService::class.java))
        }

        fun isEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false)

        private fun setEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).apply()
        }
    }
}
