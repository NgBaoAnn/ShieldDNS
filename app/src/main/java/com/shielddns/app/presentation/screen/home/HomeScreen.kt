package com.shielddns.app.presentation.screen.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shielddns.app.presentation.state.VpnConnectionState
import com.shielddns.app.presentation.theme.*

/**
 * Home Screen with VPN toggle and stats.
 */
@Composable
fun HomeScreen(
    onVpnToggle: (Boolean) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is HomeUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is HomeUiState.Ready -> {
            HomeContent(
                vpnState = state.vpnState,
                blockedCount = state.blockedCount,
                onVpnToggle = onVpnToggle
            )
        }
        is HomeUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    vpnState: VpnConnectionState,
    blockedCount: Long,
    onVpnToggle: (Boolean) -> Unit
) {
    val isConnected = vpnState is VpnConnectionState.Connected
    val isConnecting = vpnState is VpnConnectionState.Connecting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Title
        Text(
            text = "ShieldDNS",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "DNS-Based Ad Blocker",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Shield Button
        ShieldToggleButton(
            isConnected = isConnected,
            isConnecting = isConnecting,
            onClick = { onVpnToggle(!isConnected) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Status Text
        Text(
            text = when {
                isConnecting -> "Connecting..."
                isConnected -> "Protected"
                vpnState is VpnConnectionState.Error -> "Error: ${vpnState.reason}"
                else -> "Not Protected"
            },
            style = MaterialTheme.typography.titleMedium,
            color = when {
                isConnected -> ShieldActive
                isConnecting -> ShieldConnecting
                vpnState is VpnConnectionState.Error -> BlockedRed
                else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            }
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Blocked",
                value = formatNumber(blockedCount),
                subtitle = "ads & trackers"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Saved",
                value = "${(blockedCount * 50) / 1024} MB",
                subtitle = "estimated data"
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Footer info
        Text(
            text = "Only DNS-level blocking. Cannot block server-side ads.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ShieldToggleButton(
    isConnected: Boolean,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isConnected) 1.1f else 1f,
        animationSpec = tween(300),
        label = "scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isConnecting -> ShieldConnecting.copy(alpha = 0.2f)
            isConnected -> ShieldActive.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300),
        label = "bgColor"
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            isConnecting -> ShieldConnecting
            isConnected -> ShieldActive
            else -> ShieldInactive
        },
        animationSpec = tween(300),
        label = "iconColor"
    )

    FilledIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(180.dp)
            .scale(scale),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = backgroundColor
        ),
        enabled = !isConnecting
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = if (isConnected) "Disable VPN" else "Enable VPN",
            modifier = Modifier.size(80.dp),
            tint = iconColor
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatNumber(num: Long): String {
    return when {
        num >= 1_000_000 -> String.format("%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format("%.1fK", num / 1_000.0)
        else -> num.toString()
    }
}
