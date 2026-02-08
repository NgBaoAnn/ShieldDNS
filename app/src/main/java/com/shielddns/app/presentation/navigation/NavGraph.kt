package com.shielddns.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.shielddns.app.presentation.screen.home.HomeScreen
import com.shielddns.app.presentation.screen.stats.StatsScreen

/**
 * Navigation graph for the app.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    onVpnToggle: (Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(onVpnToggle = onVpnToggle)
        }

        composable(Screen.Stats.route) {
            StatsScreen()
        }

        composable(Screen.Settings.route) {
            // TODO: Implement SettingsScreen
            // For now, placeholder composable
            androidx.compose.material3.Text("Settings coming soon")
        }
    }
}
