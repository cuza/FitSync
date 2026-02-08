package dev.cuza.FitSync.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.cuza.FitSync.presentation.screen.HomeScreen
import dev.cuza.FitSync.presentation.screen.SettingsScreen
import dev.cuza.FitSync.presentation.viewmodel.MainUiState
import dev.cuza.FitSync.presentation.viewmodel.MainViewModel

private enum class TopRoute(
    val route: String,
    val label: String,
) {
    HOME("home", "Home"),
    SETTINGS("settings", "Settings"),
}

@Composable
fun AppNav(
    modifier: Modifier = Modifier,
    uiState: MainUiState,
    viewModel: MainViewModel,
) {
    val navController = rememberNavController()
    val items = TopRoute.entries

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            if (item == TopRoute.HOME) {
                                Icon(Icons.Filled.Home, contentDescription = item.label)
                            } else {
                                Icon(Icons.Filled.Settings, contentDescription = item.label)
                            }
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopRoute.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopRoute.HOME.route) {
                HomeScreen(
                    uiState = uiState,
                    statusLabel = viewModel::statusLabel,
                    onRequestStravaLogin = viewModel::stravaLoginIntent,
                    onLogoutStrava = viewModel::logoutStrava,
                    onPermissionResult = viewModel::onPermissionResult,
                    onScan = viewModel::scanSessions,
                    onSync = viewModel::syncNow,
                )
            }
            composable(TopRoute.SETTINGS.route) {
                SettingsScreen(
                    uiState = uiState,
                    exerciseTypeLabel = viewModel::exerciseTypeLabel,
                    onDaysBackChange = viewModel::updateDaysBack,
                    onReuploadChanged = viewModel::updateReuploadPolicy,
                    onOverrideChange = viewModel::setOverride,
                )
            }
        }
    }
}
