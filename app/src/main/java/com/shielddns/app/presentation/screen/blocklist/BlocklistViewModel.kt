package com.shielddns.app.presentation.screen.blocklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shielddns.app.domain.model.CustomRule
import com.shielddns.app.domain.model.RuleType
import com.shielddns.app.domain.repository.BlocklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for Blocklist screen.
 */
data class BlocklistUiState(
    val whitelistRules: List<CustomRule> = emptyList(),
    val blacklistRules: List<CustomRule> = emptyList(),
    val selectedTab: Int = 0, // 0 = Whitelist, 1 = Blacklist
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val domainInput: String = "",
    val inputError: String? = null
)

/**
 * ViewModel for Blocklist screen.
 */
@HiltViewModel
class BlocklistViewModel @Inject constructor(
    private val blocklistRepository: BlocklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlocklistUiState())
    val uiState: StateFlow<BlocklistUiState> = _uiState.asStateFlow()

    init {
        loadRules()
    }

    private fun loadRules() {
        viewModelScope.launch {
            blocklistRepository.observeWhitelistRules().collect { rules ->
                _uiState.update { it.copy(whitelistRules = rules, isLoading = false) }
            }
        }
        viewModelScope.launch {
            blocklistRepository.observeBlacklistRules().collect { rules ->
                _uiState.update { it.copy(blacklistRules = rules) }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, domainInput = "", inputError = null) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun updateDomainInput(input: String) {
        _uiState.update { it.copy(domainInput = input, inputError = null) }
    }

    fun addRule() {
        val domain = _uiState.value.domainInput.trim().lowercase()
        
        // Validate domain
        if (domain.isBlank()) {
            _uiState.update { it.copy(inputError = "Domain cannot be empty") }
            return
        }
        
        if (!isValidDomain(domain)) {
            _uiState.update { it.copy(inputError = "Invalid domain format") }
            return
        }

        viewModelScope.launch {
            // Check if already exists
            if (blocklistRepository.ruleExists(domain)) {
                _uiState.update { it.copy(inputError = "Domain already exists") }
                return@launch
            }

            // Add based on current tab
            val isWhitelist = _uiState.value.selectedTab == 0
            if (isWhitelist) {
                blocklistRepository.addToWhitelist(domain)
            } else {
                blocklistRepository.addToBlacklist(domain)
            }
            
            hideAddDialog()
        }
    }

    fun removeRule(rule: CustomRule) {
        viewModelScope.launch {
            blocklistRepository.removeRule(rule.domain)
        }
    }

    private fun isValidDomain(domain: String): Boolean {
        // Simple domain validation
        val domainPattern = Regex("^([a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$")
        return domain.matches(domainPattern) || domain.startsWith("*.")
    }
}
