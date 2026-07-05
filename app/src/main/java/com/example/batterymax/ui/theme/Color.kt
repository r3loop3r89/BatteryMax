package com.example.batterymax.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val BatteryHigh = Color(0xFF43A047)
val BatteryMedium = Color(0xFFFFA000)
val BatteryLow = Color(0xFFE53935)
val StatusConnected = Color(0xFF43A047)

@Composable
fun batteryLevelColor(level: Int?, dimmed: Boolean = false): Color {
    if (dimmed || level == null) return MaterialTheme.colorScheme.onSurfaceVariant
    return when {
        level >= 50 -> BatteryHigh
        level >= 20 -> BatteryMedium
        else -> BatteryLow
    }
}