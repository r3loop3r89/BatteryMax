package com.example.batterymax.ui.graph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.batterymax.data.db.BatterySampleEntity
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState

@Composable
fun GraphScreen(viewModel: GraphViewModel) {
    val day by viewModel.day.collectAsState()
    val state by viewModel.uiState.collectAsState()

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(state.series, state.selectedSourceId) {
        val series = state.series ?: return@LaunchedEffect
        modelProducer.runTransaction {
            lineSeries {
                series(series.x, series.y)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Text(
            "Devices",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.sources, key = { it.id }) { source ->
                val selected = source.id == state.selectedSourceId
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.selectSource(source.id) },
                    label = { Text(source.label) },
                    leadingIcon = {
                        Icon(
                            if (source.id == BatterySampleEntity.SOURCE_PHONE) {
                                Icons.Default.BatteryStd
                            } else {
                                Icons.Default.Bluetooth
                            },
                            contentDescription = null
                        )
                    }
                )
            }
        }

        Text(
            "Zoom",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(GraphZoomPreset.entries, key = { it.name }) { preset ->
                FilterChip(
                    selected = preset == state.zoomPreset,
                    onClick = { viewModel.selectZoomPreset(preset) },
                    label = { Text(preset.label) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::previousDay) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
            }
            Text(
                if (day.isToday) "Today" else day.label(),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = viewModel::nextDay, enabled = !day.isToday) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
            }
        }

        Text(
            state.selectedLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        val series = state.series
        if (series == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No samples recorded for ${state.selectedLabel} on this day",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            key(state.selectedSourceId, day.startMillis, state.zoomPreset) {
                BatteryChart(
                    modelProducer = modelProducer,
                    series = series,
                    zoomPreset = state.zoomPreset
                )
            }
        }
    }
}

@Composable
private fun BatteryChart(
    modelProducer: CartesianChartModelProducer,
    series: GraphSeries,
    zoomPreset: GraphZoomPreset
) {
    val rangeProvider = remember(zoomPreset, series.firstX, series.lastX) {
        rangeProviderFor(zoomPreset, series)
    }
    val initialZoom = remember(zoomPreset) { initialZoomFor(zoomPreset) }
    val initialScroll = remember(zoomPreset) { initialScrollFor(zoomPreset) }

    val zoomState = rememberVicoZoomState(
        zoomEnabled = true,
        initialZoom = initialZoom,
        minZoom = Zoom.min(Zoom.Content, Zoom.x(24.0)),
        maxZoom = Zoom.max(Zoom.fixed(20f), Zoom.x(0.25))
    )
    val scrollState = rememberVicoScrollState(
        scrollEnabled = true,
        initialScroll = initialScroll
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(rangeProvider = rangeProvider),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    "${value.toInt()}%"
                }
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    val hours = value.toInt().coerceIn(0, 23)
                    val minutes = ((value - hours) * 60).toInt().coerceIn(0, 59)
                    "%02d:%02d".format(hours, minutes)
                }
            )
        ),
        modelProducer = modelProducer,
        scrollState = scrollState,
        zoomState = zoomState,
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

private fun rangeProviderFor(
    zoomPreset: GraphZoomPreset,
    series: GraphSeries
): CartesianLayerRangeProvider =
    when (zoomPreset) {
        GraphZoomPreset.FitToData -> {
            val minX = series.firstX
            val maxX = if (series.lastX > series.firstX) {
                series.lastX
            } else {
                // Single point: give the axis a small width so Vico can render.
                (series.firstX + 0.25).coerceAtMost(24.0)
            }
            CartesianLayerRangeProvider.fixed(
                minX = minX,
                maxX = maxX,
                minY = 0.0,
                maxY = 100.0
            )
        }
        GraphZoomPreset.OneHour,
        GraphZoomPreset.ThreeHours,
        GraphZoomPreset.FullDay ->
            CartesianLayerRangeProvider.fixed(
                minX = 0.0,
                maxX = 24.0,
                minY = 0.0,
                maxY = 100.0
            )
    }

private fun initialZoomFor(zoomPreset: GraphZoomPreset): Zoom =
    when (zoomPreset) {
        GraphZoomPreset.OneHour -> Zoom.x(1.0)
        GraphZoomPreset.ThreeHours -> Zoom.x(3.0)
        GraphZoomPreset.FullDay -> Zoom.Content
        GraphZoomPreset.FitToData -> Zoom.Content
    }

private fun initialScrollFor(zoomPreset: GraphZoomPreset): Scroll.Absolute =
    when (zoomPreset) {
        // Show the latest / current end of the day window.
        GraphZoomPreset.OneHour,
        GraphZoomPreset.ThreeHours -> Scroll.Absolute.End
        GraphZoomPreset.FullDay,
        GraphZoomPreset.FitToData -> Scroll.Absolute.Start
    }
