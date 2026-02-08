package com.shielddns.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.shielddns.app.presentation.screen.apps.AppsScreen
import com.shielddns.app.presentation.screen.blocklist.BlocklistScreen
import com.shielddns.app.presentation.screen.home.HomeScreen
import com.shielddns.app.presentation.screen.settings.SettingsScreen
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
            SettingsScreen(
                onNavigateToBlocklist = {
                    navController.navigate(Screen.Blocklist.route)
                },
                onNavigateToApps = {
                    navController.navigate(Screen.Apps.route)
                }
            )
        }

        composable(Screen.Blocklist.route) {
            BlocklistScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Apps.route) {
            AppsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

