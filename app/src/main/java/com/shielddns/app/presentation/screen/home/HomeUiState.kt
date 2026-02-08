package com.shielddns.app.presentation.screen.home

import com.shielddns.app.domain.model.BlockStats
import com.shielddns.app.domain.model.BlockedDomain
import com.shielddns.app.presentation.state.VpnConnectionState

/**
 * UI State for Home Screen.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    
    data class Ready(
        val vpnState: VpnConnectionState,
        val blockedCount: Long,
        val recentBlocks: List<BlockedDomain> = emptyList()
    ) : HomeUiState
    
    data class Error(val message: String) : HomeUiState
}
