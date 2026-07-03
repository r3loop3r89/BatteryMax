package com.example.batterymax.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.batterymax.data.BatteryRepository
import com.example.batterymax.data.db.BatterySampleEntity
import com.example.batterymax.data.db.TrackedDeviceEntity
import com.example.batterymax.util.DayRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class GraphSeries(
    /** Hour-of-day fractions (0..24) for the x axis. */
    val x: List<Double>,
    /** Battery percentages for the y axis. */
    val y: List<Double>
)

data class GraphUiState(
    val phoneSeries: GraphSeries? = null,
    val btSeries: GraphSeries? = null,
    val btDeviceName: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class GraphViewModel(private val repository: BatteryRepository) : ViewModel() {

    private val _day = MutableStateFlow(DayRange.today())
    val day: StateFlow<DayRange> = _day

    private var trackedDevice: TrackedDeviceEntity? = null

    val uiState: StateFlow<GraphUiState> = _day
        .flatMapLatest { range -> repository.samplesBetween(range.startMillis, range.endMillis) }
        .flatMapLatest { samples ->
            kotlinx.coroutines.flow.combine(
                kotlinx.coroutines.flow.flowOf(samples),
                repository.trackedDevice
            ) { s, device ->
                trackedDevice = device
                buildState(s, device)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GraphUiState())

    private fun buildState(
        samples: List<BatterySampleEntity>,
        device: TrackedDeviceEntity?
    ): GraphUiState {
        val dayStart = _day.value.startMillis
        fun toSeries(list: List<BatterySampleEntity>): GraphSeries? {
            if (list.isEmpty()) return null
            return GraphSeries(
                x = list.map { (it.timestamp - dayStart) / 3_600_000.0 },
                y = list.map { it.levelPercent.toDouble() }
            )
        }
        return GraphUiState(
            phoneSeries = toSeries(
                samples.filter { it.source == BatterySampleEntity.SOURCE_PHONE }
            ),
            btSeries = device?.let { d -> toSeries(samples.filter { it.source == d.address }) },
            btDeviceName = device?.name
        )
    }

    fun previousDay() {
        _day.value = _day.value.previous()
    }

    fun nextDay() {
        if (!_day.value.isToday) _day.value = _day.value.next()
    }
}
