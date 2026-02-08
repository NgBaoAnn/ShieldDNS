package com.shielddns.app.presentation.state

/**
 * Represents the current VPN connection state.
 */
sealed interface VpnConnectionState {
    data object Disconnected : VpnConnectionState
    data object Connecting : VpnConnectionState
    data object Connected : VpnConnectionState
    data class Error(val reason: String) : VpnConnectionState
}
