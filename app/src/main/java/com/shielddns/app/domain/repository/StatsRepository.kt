package com.shielddns.app.domain.repository

import com.shielddns.app.domain.model.BlockStats
import com.shielddns.app.domain.model.BlockedDomain
import com.shielddns.app.domain.model.DomainCount
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for statistics operations.
 * Part of domain layer - defines contracts without implementation details.
 */
interface StatsRepository {

    /**
     * Record a blocked DNS query.
     * @param domain The blocked domain name
     */
    suspend fun recordBlockedQuery(domain: String)

    /**
     * Record multiple blocked queries in batch (for performance).
     * @param domains List of blocked domain names
     */
    suspend fun recordBlockedQueries(domains: List<String>)

    /**
     * Get today's block statistics.
     * @return Flow of BlockStats for today
     */
    fun getTodayStats(): Flow<BlockStats>

    /**
     * Get blocked count for today.
     * @return Flow of blocked count
     */
    fun getBlockedCountToday(): Flow<Long>

    /**
     * Get total blocked count all-time.
     * @return Flow of total blocked count
     */
    fun getTotalBlocked(): Flow<Long>

    /**
     * Get top N blocked domains.
     * @param limit Maximum number of domains to return
     * @param sinceDays Number of days to look back (0 = today only)
     * @return Flow of domain counts sorted by count descending
     */
    fun getTopBlockedDomains(limit: Int = 10, sinceDays: Int = 7): Flow<List<DomainCount>>

    /**
     * Get data saved in bytes.
     * @param sinceDays Number of days to look back
     * @return Flow of data saved in bytes
     */
    fun getDataSavedBytes(sinceDays: Int = 7): Flow<Long>

    /**
     * Get recent blocked domains.
     * @param limit Maximum number of entries to return
     * @return Flow of recent blocked domains
     */
    fun getRecentBlocked(limit: Int = 20): Flow<List<BlockedDomain>>

    /**
     * Get daily stats for the past N days.
     * @param days Number of days
     * @return Flow of daily stats list
     */
    fun getDailyStats(days: Int = 7): Flow<List<DailyBlockStats>>

    /**
     * Clean up old data to save storage.
     * @param olderThanDays Delete data older than this many days
     * @return Number of records deleted
     */
    suspend fun cleanupOldData(olderThanDays: Int = 30): Int
}

/**
 * Statistics for a single day.
 */
data class DailyBlockStats(
    val date: String,
    val blockedCount: Long,
    val dataSavedBytes: Long
)
