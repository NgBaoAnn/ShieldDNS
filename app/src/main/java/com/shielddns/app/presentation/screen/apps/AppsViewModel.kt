package com.shielddns.app.presentation.screen.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shielddns.app.domain.model.InstalledApp
import com.shielddns.app.domain.repository.AppFilterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for Apps screen.
 */
data class AppsUiState(
    val apps: List<InstalledApp> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filteredApps: List<InstalledApp> = emptyList()
)

/**
 * ViewModel for per-app filtering screen.
 */
@HiltViewModel
class AppsViewModel @Inject constructor(
    private val appFilterRepository: AppFilterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    init {
        loadApps()
        observeApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            appFilterRepository.loadInstalledApps()
        }
    }

    private fun observeApps() {
        viewModelScope.launch {
            appFilterRepository.observeAllApps().collect { apps ->
                _uiState.update { state ->
                    val filtered = filterApps(apps, state.searchQuery)
                    state.copy(
                        apps = apps,
                        filteredApps = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = filterApps(state.apps, query)
            state.copy(searchQuery = query, filteredApps = filtered)
        }
    }

    fun toggleAppFilter(packageName: String) {
        viewModelScope.launch {
            appFilterRepository.toggleAppFilter(packageName)
        }
    }

    private fun filterApps(apps: List<InstalledApp>, query: String): List<InstalledApp> {
        if (query.isBlank()) return apps
        val lowercaseQuery = query.lowercase()
        return apps.filter { app ->
            app.appName.lowercase().contains(lowercaseQuery) ||
            app.packageName.lowercase().contains(lowercaseQuery)
        }
    }
}
