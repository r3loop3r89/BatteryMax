package com.example.batterymax.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.batterymax.bluetooth.BtBatteryReader
import com.example.batterymax.data.BatteryRepository
import com.example.batterymax.data.db.TrackedDeviceEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BondedDevice(
    val address: String,
    val name: String,
    val connected: Boolean
)

class DevicesViewModel(
    private val repository: BatteryRepository,
    private val btReader: BtBatteryReader
) : ViewModel() {

    private val _bondedDevices = MutableStateFlow<List<BondedDevice>>(emptyList())
    val bondedDevices: StateFlow<List<BondedDevice>> = _bondedDevices.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val trackedDevices: StateFlow<List<TrackedDeviceEntity>> = repository.trackedDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun hasBluetoothPermission(): Boolean = btReader.hasPermission()

    fun refreshBondedDevices() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                _bondedDevices.value = btReader.bondedDevices().map { device ->
                    val name = try {
                        device.name ?: device.address
                    } catch (_: SecurityException) {
                        device.address
                    }
                    BondedDevice(
                        address = device.address,
                        name = name,
                        connected = btReader.isConnected(device)
                    )
                }.sortedWith(
                    compareByDescending<BondedDevice> { it.connected }
                        .thenBy { it.name.lowercase() }
                )
                delay(250)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun selectDevice(device: BondedDevice) {
        viewModelScope.launch { repository.addTrackedDevice(device.address, device.name) }
    }

    fun removeDevice(address: String) {
        viewModelScope.launch { repository.removeTrackedDevice(address) }
    }
}
