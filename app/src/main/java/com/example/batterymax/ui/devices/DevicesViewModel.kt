package com.example.batterymax.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.batterymax.bluetooth.BtBatteryReader
import com.example.batterymax.data.BatteryRepository
import com.example.batterymax.data.db.TrackedDeviceEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BondedDevice(val address: String, val name: String)

class DevicesViewModel(
    private val repository: BatteryRepository,
    private val btReader: BtBatteryReader
) : ViewModel() {

    private val _bondedDevices = MutableStateFlow<List<BondedDevice>>(emptyList())
    val bondedDevices: StateFlow<List<BondedDevice>> = _bondedDevices

    val trackedDevice: StateFlow<TrackedDeviceEntity?> = repository.trackedDevice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun hasBluetoothPermission(): Boolean = btReader.hasPermission()

    fun refreshBondedDevices() {
        _bondedDevices.value = btReader.bondedDevices().map { device ->
            val name = try {
                device.name ?: device.address
            } catch (_: SecurityException) {
                device.address
            }
            BondedDevice(address = device.address, name = name)
        }.sortedBy { it.name.lowercase() }
    }

    fun selectDevice(device: BondedDevice) {
        viewModelScope.launch { repository.setTrackedDevice(device.address, device.name) }
    }

    fun clearDevice() {
        viewModelScope.launch { repository.clearTrackedDevice() }
    }
}
