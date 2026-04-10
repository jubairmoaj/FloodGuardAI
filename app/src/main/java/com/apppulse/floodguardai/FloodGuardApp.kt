package com.apppulse.floodguardai

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Traffic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.apppulse.floodguardai.ui.screens.AlertsScreen
import com.apppulse.floodguardai.ui.screens.ChatScreen
import com.apppulse.floodguardai.ui.screens.DashboardScreen
import com.apppulse.floodguardai.ui.screens.MapScreen
import com.apppulse.floodguardai.ui.screens.ReportScreen
import com.apppulse.floodguardai.ui.screens.RoutePlannerScreen
import com.apppulse.floodguardai.ui.screens.SettingsScreen
import com.apppulse.floodguardai.ui.theme.Cyan400
import com.apppulse.floodguardai.ui.theme.Navy800
import com.apppulse.floodguardai.ui.theme.OnDark
import com.apppulse.floodguardai.ui.theme.OnDarkMuted
import com.apppulse.floodguardai.ui.viewmodel.MainViewModel

enum class AppDestination(val route: String, val title: String) {
    Dashboard("dashboard", "Home"),
    Map("map", "Map"),
    Chat("chat", "Ask AI"),
    Route("route", "Route"),
    Report("report", "Report"),
    Alerts("alerts", "Alerts"),
    Settings("settings", "Settings")
}

@Composable
fun FloodGuardApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val destinations = AppDestination.entries

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = Navy800,
                tonalElevation = androidx.compose.ui.unit.Dp.Unspecified
            ) {
                destinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (destination) {
                                    AppDestination.Dashboard -> Icons.Outlined.Home
                                    AppDestination.Map       -> Icons.Outlined.Map
                                    AppDestination.Chat      -> Icons.Outlined.Chat
                                    AppDestination.Route     -> Icons.Outlined.Traffic
                                    AppDestination.Report    -> Icons.Outlined.Place
                                    AppDestination.Alerts    -> Icons.Outlined.Notifications
                                    AppDestination.Settings  -> Icons.Outlined.Person
                                },
                                contentDescription = destination.title
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontSize = 10.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor      = Cyan400,
                            selectedTextColor      = Cyan400,
                            indicatorColor         = Cyan400.copy(alpha = 0.15f),
                            unselectedIconColor    = OnDarkMuted,
                            unselectedTextColor    = OnDarkMuted
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(AppDestination.Dashboard.route) {
                DashboardScreen(viewModel = viewModel)
            }
            composable(AppDestination.Map.route) {
                MapScreen(viewModel = viewModel)
            }
            composable(AppDestination.Chat.route) {
                ChatScreen(viewModel = viewModel)
            }
            composable(AppDestination.Route.route) {
                RoutePlannerScreen(viewModel = viewModel)
            }
            composable(AppDestination.Report.route) {
                ReportScreen(viewModel = viewModel)
            }
            composable(AppDestination.Alerts.route) {
                AlertsScreen(viewModel = viewModel)
            }
            composable(AppDestination.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
