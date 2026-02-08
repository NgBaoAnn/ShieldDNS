package com.shielddns.app.presentation.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shielddns.app.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel for Stats Screen.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.TODAY)

    val uiState: StateFlow<StatsUiState> = combine(
        _selectedPeriod,
        statsRepository.getBlockedCountToday(),
        statsRepository.getTotalBlocked(),
        statsRepository.getTopBlockedDomains(limit = 10, sinceDays = 7),
        statsRepository.getDataSavedBytes(sinceDays = 7),
        statsRepository.getDailyStats(days = 7)
    ) { values ->
        val period = values[0] as StatsPeriod
        val blockedToday = values[1] as Long
        val totalBlocked = values[2] as Long
        @Suppress("UNCHECKED_CAST")
        val topDomains = values[3] as List<com.shielddns.app.domain.model.DomainCount>
        val dataSaved = values[4] as Long
        @Suppress("UNCHECKED_CAST")
        val dailyStats = values[5] as List<com.shielddns.app.domain.repository.DailyBlockStats>

        StatsUiState(
            isLoading = false,
            selectedPeriod = period,
            blockedToday = blockedToday,
            blockedThisWeek = dailyStats.sumOf { it.blockedCount },
            totalBlocked = totalBlocked,
            dataSavedKb = dataSaved / 1024,
            topBlockedDomains = topDomains,
            dailyStats = dailyStats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )

    fun selectPeriod(period: StatsPeriod) {
        _selectedPeriod.update { period }
    }
}
