package com.example.batterymax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.batterymax.bluetooth.BtBatteryReader
import com.example.batterymax.data.BatteryRepository
import com.example.batterymax.ui.dashboard.DashboardScreen
import com.example.batterymax.ui.dashboard.DashboardViewModel
import com.example.batterymax.ui.devices.DevicesScreen
import com.example.batterymax.ui.devices.DevicesViewModel
import com.example.batterymax.ui.graph.GraphScreen
import com.example.batterymax.ui.graph.GraphViewModel
import com.example.batterymax.ui.settings.SettingsScreen
import com.example.batterymax.ui.theme.BatteryMaxTheme

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    Destination("dashboard", "Dashboard", Icons.Default.BatteryStd),
    Destination("graph", "Graph", Icons.AutoMirrored.Filled.ShowChart),
    Destination("devices", "Devices", Icons.Default.Bluetooth),
    Destination("settings", "Settings", Icons.Default.Settings)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as BatteryMaxApp).repository
        val factory = AppViewModelFactory(repository, BtBatteryReader(applicationContext))

        setContent {
            BatteryMaxTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            destinations.forEach { destination ->
                                NavigationBarItem(
                                    selected = currentRoute == destination.route,
                                    onClick = {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(destination.icon, contentDescription = null)
                                    },
                                    label = { Text(destination.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("dashboard") {
                            DashboardScreen(viewModel(factory = factory))
                        }
                        composable("graph") {
                            GraphScreen(viewModel(factory = factory))
                        }
                        composable("devices") {
                            DevicesScreen(viewModel(factory = factory))
                        }
                        composable("settings") {
                            SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}

private class AppViewModelFactory(
    private val repository: BatteryRepository,
    private val btReader: BtBatteryReader
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
            DashboardViewModel(repository) as T
        modelClass.isAssignableFrom(GraphViewModel::class.java) ->
            GraphViewModel(repository) as T
        modelClass.isAssignableFrom(DevicesViewModel::class.java) ->
            DevicesViewModel(repository, btReader) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
