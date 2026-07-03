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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.batterymax.BatteryMaxApp
import com.example.batterymax.data.db.BatterySampleEntity
import com.example.batterymax.util.TimeFormats
import java.util.Calendar
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
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent

@Composable
fun GraphScreen(viewModel: GraphViewModel) {
    val context = LocalContext.current
    val preferences = (context.applicationContext as BatteryMaxApp).preferences
    val use24Hour by preferences.use24HourClock.collectAsState()
    val day by viewModel.day.collectAsState()
    val state by viewModel.uiState.collectAsState()

    val modelProducer = remember { CartesianChartModelProducer() }
    var scrollToNowToken by remember { mutableIntStateOf(0) }

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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (day.isToday) "Today" else day.label(),
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = {
                        if (!day.isToday) viewModel.goToToday()
                        scrollToNowToken++
                    },
                    enabled = state.series != null
                ) {
                    Text("Now")
                }
            }
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
            key(state.selectedSourceId, day.startMillis, state.zoomPreset, use24Hour) {
                BatteryChart(
                    modelProducer = modelProducer,
                    series = series,
                    zoomPreset = state.zoomPreset,
                    scrollToNowToken = scrollToNowToken,
                    use24Hour = use24Hour
                )
            }
        }
    }
}

@Composable
private fun BatteryChart(
    modelProducer: CartesianChartModelProducer,
    series: GraphSeries,
    zoomPreset: GraphZoomPreset,
    scrollToNowToken: Int,
    use24Hour: Boolean
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

    LaunchedEffect(scrollToNowToken) {
        if (scrollToNowToken == 0) return@LaunchedEffect
        scrollState.animateScroll(scrollTargetForNow(zoomPreset))
    }

    val markerLabelBackground = rememberShapeComponent(
        fill = Fill(MaterialTheme.colorScheme.surfaceContainerHighest),
        shape = RoundedCornerShape(8.dp)
    )
    val markerLabel = rememberTextComponent(
        style = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp
        ),
        padding = Insets(horizontal = 8.dp, vertical = 4.dp),
        background = markerLabelBackground
    )
    val markerGuideline = rememberLineComponent(
        fill = Fill(MaterialTheme.colorScheme.outline)
    )
    val markerValueFormatter = remember(use24Hour) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val target = targets.firstOrNull() ?: return@ValueFormatter ""
            val y = (target as? LineCartesianLayerMarkerTarget)
                ?.points
                ?.firstOrNull()
                ?.entry
                ?.y
                ?: return@ValueFormatter ""
            "%d%% at %s".format(
                y.toInt(),
                TimeFormats.formatHourOfDay(target.x, use24Hour)
            )
        }
    }
    val marker = rememberDefaultCartesianMarker(
        label = markerLabel,
        valueFormatter = markerValueFormatter,
        indicator = { color: Color ->
            ShapeComponent(fill = Fill(color), shape = CircleShape)
        },
        guideline = markerGuideline
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
                    TimeFormats.formatHourOfDay(value, use24Hour)
                }
            ),
            marker = marker
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

/** Scroll so the current time (or latest sample) is in view. */
private fun scrollTargetForNow(zoomPreset: GraphZoomPreset): Scroll.Absolute =
    when (zoomPreset) {
        GraphZoomPreset.FitToData -> Scroll.Absolute.End
        GraphZoomPreset.OneHour,
        GraphZoomPreset.ThreeHours,
        GraphZoomPreset.FullDay -> {
            // Put "now" at the right edge of the visible window.
            Scroll.Absolute.x(currentHourOfDay().coerceIn(0.0, 24.0), bias = 1f)
        }
    }

private fun currentHourOfDay(): Double {
    val cal = Calendar.getInstance()
    val hours = cal.get(Calendar.HOUR_OF_DAY)
    val minutes = cal.get(Calendar.MINUTE)
    val seconds = cal.get(Calendar.SECOND)
    val value = hours + minutes / 60.0 + seconds / 3600.0
    // Match sample precision used elsewhere in the graph.
    return kotlin.math.round(value * 10_000.0) / 10_000.0
}

