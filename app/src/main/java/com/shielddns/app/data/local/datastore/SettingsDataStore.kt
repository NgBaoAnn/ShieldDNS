package com.shielddns.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shielddns_settings")

/**
 * DataStore wrapper for app settings.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val VPN_AUTO_START = booleanPreferencesKey("vpn_auto_start")
        private val SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        private val UPSTREAM_DNS = stringPreferencesKey("upstream_dns")
        private val BLOCK_IPV6 = booleanPreferencesKey("block_ipv6")
        private val DARK_MODE = stringPreferencesKey("dark_mode") // "system", "light", "dark"
        
        const val DEFAULT_UPSTREAM_DNS = "8.8.8.8"
        const val DEFAULT_DARK_MODE = "system"
    }

    val vpnAutoStart: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[VPN_AUTO_START] ?: false
    }

    val showNotifications: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SHOW_NOTIFICATIONS] ?: true
    }

    val upstreamDns: Flow<String> = dataStore.data.map { prefs ->
        prefs[UPSTREAM_DNS] ?: DEFAULT_UPSTREAM_DNS
    }

    val blockIpv6: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[BLOCK_IPV6] ?: false
    }

    val darkMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[DARK_MODE] ?: DEFAULT_DARK_MODE
    }

    suspend fun setVpnAutoStart(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[VPN_AUTO_START] = enabled
        }
    }

    suspend fun setShowNotifications(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SHOW_NOTIFICATIONS] = enabled
        }
    }

    suspend fun setUpstreamDns(dns: String) {
        dataStore.edit { prefs ->
            prefs[UPSTREAM_DNS] = dns
        }
    }

    suspend fun setBlockIpv6(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[BLOCK_IPV6] = enabled
        }
    }

    suspend fun setDarkMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[DARK_MODE] = mode
        }
    }
}
