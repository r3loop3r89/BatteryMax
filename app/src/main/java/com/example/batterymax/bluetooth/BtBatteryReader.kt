package com.example.batterymax.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface BtBatteryEvent {
    data class Battery(val levelPercent: Int) : BtBatteryEvent
    data class Connection(val connected: Boolean) : BtBatteryEvent
}

/**
 * Reads the battery level of a bonded Bluetooth device using three mechanisms:
 * 1. The hidden system broadcast `android.bluetooth.device.action.BATTERY_LEVEL_CHANGED`
 *    (fired for most Classic headsets/watches).
 * 2. The hidden `BluetoothDevice.getBatteryLevel()` method via reflection for initial values.
 * 3. A BLE GATT read of the standard Battery Service (0x180F/0x2A19) as fallback,
 *    polled periodically while the device is connected.
 */
class BtBatteryReader(private val context: Context) {

    fun hasPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun bondedDevices(): List<BluetoothDevice> {
        if (!hasPermission()) return emptyList()
        val adapter = adapter() ?: return emptyList()
        return try {
            adapter.bondedDevices?.toList().orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    /** Emits battery and connection events for the device at [address] until cancelled. */
    fun watch(address: String): Flow<BtBatteryEvent> = callbackFlow {
        if (!hasPermission()) {
            close()
            return@callbackFlow
        }
        val adapter = adapter()
        if (adapter == null) {
            close()
            return@callbackFlow
        }
        val device = try {
            adapter.getRemoteDevice(address)
        } catch (_: IllegalArgumentException) {
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val eventDevice: BluetoothDevice? = androidx.core.content.IntentCompat
                    .getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                if (eventDevice?.address != address) return
                when (intent.action) {
                    ACTION_BATTERY_LEVEL_CHANGED -> {
                        val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                        if (level in 0..100) trySend(BtBatteryEvent.Battery(level))
                    }
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        trySend(BtBatteryEvent.Connection(true))
                        readBatteryByReflection(device)?.let {
                            trySend(BtBatteryEvent.Battery(it))
                        }
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                        trySend(BtBatteryEvent.Connection(false))
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ContextCompat.registerReceiver(
            context, receiver, filter, ContextCompat.RECEIVER_EXPORTED
        )

        // Initial state: connection + battery via reflection if available.
        val initiallyConnected = isConnected(device)
        trySend(BtBatteryEvent.Connection(initiallyConnected))
        if (initiallyConnected) {
            readBatteryByReflection(device)?.let { trySend(BtBatteryEvent.Battery(it)) }
        }

        // GATT fallback poll while the flow is collected.
        val gattJob = launch {
            while (isActive) {
                if (isConnected(device)) {
                    val level = readBatteryViaGatt(device)
                    if (level != null) trySend(BtBatteryEvent.Battery(level))
                }
                delay(GATT_POLL_INTERVAL_MS)
            }
        }

        awaitClose {
            gattJob.cancel()
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    private fun adapter(): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    fun isConnected(device: BluetoothDevice): Boolean = try {
        val method = device.javaClass.getMethod("isConnected")
        method.invoke(device) as? Boolean ?: false
    } catch (_: Exception) {
        false
    }

    private fun readBatteryByReflection(device: BluetoothDevice): Int? = try {
        val method = device.javaClass.getMethod("getBatteryLevel")
        (method.invoke(device) as? Int)?.takeIf { it in 0..100 }
    } catch (e: Exception) {
        Log.d(TAG, "getBatteryLevel reflection failed", e)
        null
    }

    /** Connects GATT, reads the standard battery characteristic once, then disconnects. */
    private suspend fun readBatteryViaGatt(device: BluetoothDevice): Int? {
        if (!hasPermission()) return null
        return try {
            kotlinx.coroutines.withTimeoutOrNull(GATT_TIMEOUT_MS) {
                kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                    var gattRef: BluetoothGatt? = null
                    fun finish(result: Int?) {
                        runCatching { gattRef?.close() }
                        if (cont.isActive) cont.resumeWith(Result.success(result))
                    }
                    val callback = object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(
                            gatt: BluetoothGatt, status: Int, newState: Int
                        ) {
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                gatt.discoverServices()
                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                finish(null)
                            }
                        }

                        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                            val characteristic = gatt.getService(BATTERY_SERVICE_UUID)
                                ?.getCharacteristic(BATTERY_LEVEL_UUID)
                            if (characteristic == null) {
                                finish(null)
                            } else {
                                @Suppress("DEPRECATION")
                                gatt.readCharacteristic(characteristic)
                            }
                        }

                        @Deprecated("Deprecated in API 33")
                        override fun onCharacteristicRead(
                            gatt: BluetoothGatt,
                            characteristic: BluetoothGattCharacteristic,
                            status: Int
                        ) {
                            @Suppress("DEPRECATION")
                            val value = characteristic.value
                            finish(value?.firstOrNull()?.toInt()?.takeIf { it in 0..100 })
                        }

                        override fun onCharacteristicRead(
                            gatt: BluetoothGatt,
                            characteristic: BluetoothGattCharacteristic,
                            value: ByteArray,
                            status: Int
                        ) {
                            finish(value.firstOrNull()?.toInt()?.takeIf { it in 0..100 })
                        }
                    }
                    gattRef = device.connectGatt(context, false, callback)
                    cont.invokeOnCancellation { runCatching { gattRef?.close() } }
                }
            }
        } catch (e: SecurityException) {
            Log.d(TAG, "GATT read failed: missing permission", e)
            null
        }
    }

    companion object {
        private const val TAG = "BtBatteryReader"
        const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
        private val BATTERY_SERVICE_UUID: UUID =
            UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        private val BATTERY_LEVEL_UUID: UUID =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        private const val GATT_POLL_INTERVAL_MS = 5 * 60_000L
        private const val GATT_TIMEOUT_MS = 15_000L
    }
}
