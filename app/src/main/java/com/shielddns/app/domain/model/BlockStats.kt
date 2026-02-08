package com.shielddns.app.domain.model

/**
 * Statistics for blocked ads/domains.
 */
data class BlockStats(
    val totalBlocked: Long = 0,
    val blockedToday: Long = 0,
    val topBlockedDomains: List<DomainCount> = emptyList(),
    val dataSavedKb: Long = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class DomainCount(
    val domain: String,
    val count: Long
)
