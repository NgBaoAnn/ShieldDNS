package com.shielddns.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app settings.
 * Part of domain layer - defines contracts without implementation details.
 */
interface SettingsRepository {

    /**
     * Whether VPN should auto-start on boot.
     */
    val vpnAutoStart: Flow<Boolean>

    /**
     * Whether to show VPN status notifications.
     */
    val showNotifications: Flow<Boolean>

    /**
     * Upstream DNS server address.
     */
    val upstreamDns: Flow<String>

    /**
     * Whether to block IPv6 DNS queries.
     */
    val blockIpv6: Flow<Boolean>

    /**
     * Dark mode setting: "system", "light", or "dark".
     */
    val darkMode: Flow<String>

    suspend fun setVpnAutoStart(enabled: Boolean)
    suspend fun setShowNotifications(enabled: Boolean)
    suspend fun setUpstreamDns(dns: String)
    suspend fun setBlockIpv6(enabled: Boolean)
    suspend fun setDarkMode(mode: String)
}
