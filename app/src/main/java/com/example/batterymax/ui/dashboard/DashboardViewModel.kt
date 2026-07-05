package com.example.batterymax.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.batterymax.data.BatteryRepository
import com.example.batterymax.data.db.BatterySampleEntity
import com.example.batterymax.data.db.TrackedDeviceEntity
import com.example.batterymax.service.BatteryMonitorService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class BtDeviceStatus(
    val device: TrackedDeviceEntity,
    val sample: BatterySampleEntity?,
    val connected: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(repository: BatteryRepository) : ViewModel() {

    val phone: StateFlow<BatterySampleEntity?> = repository.phoneLatest
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val btDevices: StateFlow<List<BtDeviceStatus>> = combine(
        repository.trackedDevices,
        BatteryMonitorService.connectionStates
    ) { devices, connections -> devices to connections }
        .flatMapLatest { (devices, connections) ->
            if (devices.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    devices.map { device ->
                        repository.latestFor(device.address).map { sample ->
                            BtDeviceStatus(
                                device = device,
                                sample = sample,
                                connected = connections[device.address] == true
                            )
                        }
                    }
                ) { statuses ->
                    statuses.toList().sortedWith(
                        compareByDescending<BtDeviceStatus> { it.connected }
                            .thenBy { it.device.name.lowercase() }
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val serviceRunning: StateFlow<Boolean> = BatteryMonitorService.running
}
