package com.example.batterymax.ui.graph

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.batterymax.data.BatteryRepository
import com.example.batterymax.data.db.BatterySampleEntity
import com.example.batterymax.util.DayRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class GraphSeries(
    /** Hour-of-day fractions (0..24) for the x axis. */
    val x: List<Double>,
    /** Battery percentages for the y axis. */
    val y: List<Double>
) {
    val firstX: Double get() = x.first()
    val lastX: Double get() = x.last()
}

enum class GraphZoomPreset(val label: String) {
    OneHour("1 hour"),
    ThreeHours("3 hours"),
    FullDay("100%"),
    FitToData("Zoom to fit")
}

data class GraphUiState(
    val label: String = "Phone",
    val series: GraphSeries? = null,
    val zoomPreset: GraphZoomPreset = GraphZoomPreset.FitToData
)

@OptIn(ExperimentalCoroutinesApi::class)
class GraphViewModel(
    private val repository: BatteryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sourceId: String = Uri.decode(
        savedStateHandle.get<String>("sourceId") ?: BatterySampleEntity.SOURCE_PHONE
    )

    private val _day = MutableStateFlow(DayRange.today())
    val day: StateFlow<DayRange> = _day

    private val _zoomPreset = MutableStateFlow(GraphZoomPreset.FitToData)

    val uiState: StateFlow<GraphUiState> = combine(
        _day.flatMapLatest { range ->
            repository.samplesBetween(range.startMillis, range.endMillis)
        },
        repository.trackedDevices,
        _day,
        _zoomPreset
    ) { samples, trackedDevices, day, zoomPreset ->
        val label = when (sourceId) {
            BatterySampleEntity.SOURCE_PHONE -> "Phone"
            else -> trackedDevices.find { it.address == sourceId }?.name ?: "Bluetooth device"
        }
        GraphUiState(
            label = label,
            series = toSeries(
                samples.filter { it.source == sourceId },
                day.startMillis
            ),
            zoomPreset = zoomPreset
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GraphUiState())

    private fun toSeries(list: List<BatterySampleEntity>, dayStart: Long): GraphSeries? {
        if (list.isEmpty()) return null
        // Vico allows at most four decimal places on x-values.
        return GraphSeries(
            x = list.map { sample ->
                val hours = (sample.timestamp - dayStart) / 3_600_000.0
                kotlin.math.round(hours * 10_000.0) / 10_000.0
            },
            y = list.map { it.levelPercent.toDouble() }
        )
    }

    fun selectZoomPreset(preset: GraphZoomPreset) {
        _zoomPreset.value = preset
    }

    fun previousDay() {
        _day.value = _day.value.previous()
    }

    fun nextDay() {
        if (!_day.value.isToday) _day.value = _day.value.next()
    }

    fun goToToday() {
        _day.value = DayRange.today()
    }
}