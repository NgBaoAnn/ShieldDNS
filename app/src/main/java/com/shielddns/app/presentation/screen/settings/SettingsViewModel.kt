package com.shielddns.app.presentation.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shielddns.app.domain.repository.BlocklistRepository
import com.shielddns.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for Settings screen.
 */
data class SettingsUiState(
    val vpnAutoStart: Boolean = false,
    val showNotifications: Boolean = true,
    val upstreamDns: String = "8.8.8.8",
    val blockIpv6: Boolean = false,
    val darkMode: String = "system",
    val customRuleCount: Int = 0,
    val blocklistSize: Int = 0,
    val isLoading: Boolean = true
)

/**
 * ViewModel for Settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val blocklistRepository: BlocklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeCustomRules()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Collect all settings
            launch {
                settingsRepository.vpnAutoStart.collect { value ->
                    _uiState.update { it.copy(vpnAutoStart = value) }
                }
            }
            launch {
                settingsRepository.showNotifications.collect { value ->
                    _uiState.update { it.copy(showNotifications = value) }
                }
            }
            launch {
                settingsRepository.upstreamDns.collect { value ->
                    _uiState.update { it.copy(upstreamDns = value) }
                }
            }
            launch {
                settingsRepository.blockIpv6.collect { value ->
                    _uiState.update { it.copy(blockIpv6 = value) }
                }
            }
            launch {
                settingsRepository.darkMode.collect { value ->
                    _uiState.update { it.copy(darkMode = value, isLoading = false) }
                }
            }
            
            // Get blocklist size
            launch {
                try {
                    val size = blocklistRepository.getBlockedDomainsCount()
                    _uiState.update { it.copy(blocklistSize = size) }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    private fun observeCustomRules() {
        viewModelScope.launch {
            blocklistRepository.observeRuleCount().collect { count ->
                _uiState.update { it.copy(customRuleCount = count) }
            }
        }
    }

    fun setVpnAutoStart(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVpnAutoStart(enabled)
        }
    }

    fun setShowNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowNotifications(enabled)
        }
    }

    fun setUpstreamDns(dns: String) {
        viewModelScope.launch {
            settingsRepository.setUpstreamDns(dns)
        }
    }

    fun setBlockIpv6(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBlockIpv6(enabled)
        }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(mode)
        }
    }
}
