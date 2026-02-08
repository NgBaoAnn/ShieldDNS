package com.shielddns.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shielddns.app.domain.repository.SettingsRepository
import com.shielddns.app.presentation.navigation.NavGraph
import com.shielddns.app.presentation.navigation.Screen
import com.shielddns.app.presentation.theme.ShieldDnsTheme
import com.shielddns.app.service.VpnServiceController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity for ShieldDNS.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var vpnController: VpnServiceController

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        vpnController.onPermissionResult(result.resultCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Observe dark mode setting
            val darkModePreference by settingsRepository.darkMode.collectAsState(initial = "system")
            val isSystemDark = isSystemInDarkTheme()
            
            val isDarkTheme = when (darkModePreference) {
                "light" -> false
                "dark" -> true
                else -> isSystemDark // "system" or default
            }

            ShieldDnsTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            Screen.bottomNavItems.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.route == screen.route
                                } == true

                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = { Text(screen.title) },
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavGraph(
                            navController = navController,
                            onVpnToggle = { shouldEnable ->
                                if (shouldEnable) {
                                    val prepareIntent = vpnController.prepare()
                                    if (prepareIntent != null) {
                                        vpnPermissionLauncher.launch(prepareIntent)
                                    } else {
                                        vpnController.startVpn()
                                    }
                                } else {
                                    vpnController.stopVpn()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

