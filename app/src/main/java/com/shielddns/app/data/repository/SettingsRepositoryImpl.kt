package com.shielddns.app.data.repository

import com.shielddns.app.data.local.datastore.SettingsDataStore
import com.shielddns.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository using DataStore.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override val vpnAutoStart: Flow<Boolean> = settingsDataStore.vpnAutoStart
    override val showNotifications: Flow<Boolean> = settingsDataStore.showNotifications
    override val upstreamDns: Flow<String> = settingsDataStore.upstreamDns
    override val blockIpv6: Flow<Boolean> = settingsDataStore.blockIpv6
    override val darkMode: Flow<String> = settingsDataStore.darkMode

    override suspend fun setVpnAutoStart(enabled: Boolean) {
        settingsDataStore.setVpnAutoStart(enabled)
    }

    override suspend fun setShowNotifications(enabled: Boolean) {
        settingsDataStore.setShowNotifications(enabled)
    }

    override suspend fun setUpstreamDns(dns: String) {
        settingsDataStore.setUpstreamDns(dns)
    }

    override suspend fun setBlockIpv6(enabled: Boolean) {
        settingsDataStore.setBlockIpv6(enabled)
    }

    override suspend fun setDarkMode(mode: String) {
        settingsDataStore.setDarkMode(mode)
    }
}
