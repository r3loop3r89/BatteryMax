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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(repository: BatteryRepository) : ViewModel() {

    val phone: StateFlow<BatterySampleEntity?> = repository.phoneLatest
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val trackedDevice: StateFlow<TrackedDeviceEntity?> = repository.trackedDevice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val btLatest: StateFlow<BatterySampleEntity?> = repository.trackedDevice
        .flatMapLatest { device ->
            if (device == null) flowOf(null) else repository.latestFor(device.address)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val serviceRunning: StateFlow<Boolean> = BatteryMonitorService.running
}
