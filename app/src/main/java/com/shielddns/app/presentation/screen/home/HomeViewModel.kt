package com.shielddns.app.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shielddns.app.presentation.state.VpnConnectionState
import com.shielddns.app.service.AdBlockVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for Home Screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

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
}
