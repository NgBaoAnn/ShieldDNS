package com.shielddns.app.presentation.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Settings screen with grouped settings sections.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToBlocklist: () -> Unit = {},
    onNavigateToApps: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // General Section
        SettingsSection(title = "General") {
            SwitchSettingItem(
                icon = Icons.Default.PowerSettingsNew,
                title = "Auto-start VPN",
                subtitle = "Start VPN when device boots",
                checked = uiState.vpnAutoStart,
                onCheckedChange = viewModel::setVpnAutoStart
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            SwitchSettingItem(
                icon = Icons.Default.Notifications,
                title = "Show Notifications",
                subtitle = "Display VPN status notifications",
                checked = uiState.showNotifications,
                onCheckedChange = viewModel::setShowNotifications
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DNS Section
        SettingsSection(title = "DNS") {
            ClickableSettingItem(
                icon = Icons.Default.Dns,
                title = "Upstream DNS",
                subtitle = uiState.upstreamDns,
                onClick = {
                    // TODO: Show DNS picker dialog
                }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            SwitchSettingItem(
                icon = Icons.Default.Security,
                title = "Block IPv6",
                subtitle = "Block IPv6 DNS queries",
                checked = uiState.blockIpv6,
                onCheckedChange = viewModel::setBlockIpv6
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Appearance Section
        SettingsSection(title = "Appearance") {
            ClickableSettingItem(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                subtitle = when (uiState.darkMode) {
                    "light" -> "Light"
                    "dark" -> "Dark"
                    else -> "System default"
                },
                onClick = {
                    // Toggle between modes
                    val nextMode = when (uiState.darkMode) {
                        "system" -> "light"
                        "light" -> "dark"
                        else -> "system"
                    }
                    viewModel.setDarkMode(nextMode)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Blocklist Section
        SettingsSection(title = "Blocklist") {
            ClickableSettingItem(
                icon = Icons.AutoMirrored.Filled.List,
                title = "Custom Rules",
                subtitle = "${uiState.customRuleCount} custom rules • ${uiState.blocklistSize} blocked domains",
                onClick = onNavigateToBlocklist
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Filtering Section
        SettingsSection(title = "App Filtering") {
            ClickableSettingItem(
                icon = Icons.Default.Apps,
                title = "Per-App Filtering",
                subtitle = "Choose which apps use DNS filtering",
                onClick = onNavigateToApps
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Section
        SettingsSection(title = "About") {
            ClickableSettingItem(
                icon = Icons.Default.Info,
                title = "ShieldDNS",
                subtitle = "Version 1.0.0",
                onClick = { }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun SwitchSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ClickableSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
