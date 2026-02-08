package com.shielddns.app.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shielddns.app.data.local.datastore.SettingsDataStore
import com.shielddns.app.presentation.state.VpnConnectionState
import com.shielddns.app.service.AdBlockVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home Screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _showCacheClearDialog = MutableStateFlow(false)
    val showCacheClearDialog: StateFlow<Boolean> = _showCacheClearDialog.asStateFlow()

    // Callback to store for after dialog is handled
    private var pendingVpnToggle: ((Boolean) -> Unit)? = null

    val uiState: StateFlow<HomeUiState> = combine(
        AdBlockVpnService.connectionState,
        AdBlockVpnService.blockedCount
    ) { vpnState, blockedCount ->
        HomeUiState.Ready(
            vpnState = vpnState,
            blockedCount = blockedCount,
            recentBlocks = emptyList()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Ready(
            vpnState = VpnConnectionState.Disconnected,
            blockedCount = 0
        )
    )

    /**
     * Called when user tries to enable VPN.
     * Shows cache clear dialog on first enable.
     */
    fun onVpnEnableRequested(onVpnToggle: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isFirstTime = settingsDataStore.checkIsFirstVpnEnable()
            if (isFirstTime) {
                pendingVpnToggle = onVpnToggle
                _showCacheClearDialog.value = true
            } else {
                // Not first time, just enable VPN directly
                onVpnToggle(true)
            }
        }
    }

    /**
     * Dismiss the cache clear dialog without action.
     */
    fun dismissCacheClearDialog() {
        _showCacheClearDialog.value = false
        // Still enable VPN after user has seen the dialog
        viewModelScope.launch {
            settingsDataStore.setFirstVpnEnableCompleted()
            pendingVpnToggle?.invoke(true)
            pendingVpnToggle = null
        }
    }

    /**
     * Skip cache clear and enable VPN.
     */
    fun skipCacheClear() {
        _showCacheClearDialog.value = false
        viewModelScope.launch {
            settingsDataStore.setFirstVpnEnableCompleted()
            pendingVpnToggle?.invoke(true)
            pendingVpnToggle = null
        }
    }
}

