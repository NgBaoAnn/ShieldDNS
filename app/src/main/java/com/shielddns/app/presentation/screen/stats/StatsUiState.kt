package com.shielddns.app.presentation.screen.stats

import com.shielddns.app.domain.model.DomainCount
import com.shielddns.app.domain.repository.DailyBlockStats

/**
 * UI State for Stats Screen.
 */
data class StatsUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: StatsPeriod = StatsPeriod.TODAY,
    val blockedToday: Long = 0,
    val blockedThisWeek: Long = 0,
    val totalBlocked: Long = 0,
    val dataSavedKb: Long = 0,
    val topBlockedDomains: List<DomainCount> = emptyList(),
    val dailyStats: List<DailyBlockStats> = emptyList(),
    val error: String? = null
)

/**
 * Time period for stats display.
 */
enum class StatsPeriod(val label: String, val days: Int) {
    TODAY("Today", 0),
    WEEK("This Week", 7),
    MONTH("This Month", 30)
}
