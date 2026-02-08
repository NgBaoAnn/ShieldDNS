package com.shielddns.app.data.repository

import com.shielddns.app.data.local.database.dao.BlockedQueryDao
import com.shielddns.app.data.local.database.dao.DailyStatsDao
import com.shielddns.app.data.local.database.entity.BlockedQueryEntity
import com.shielddns.app.data.local.database.entity.DailyStatsEntity
import com.shielddns.app.domain.model.BlockStats
import com.shielddns.app.domain.model.BlockedDomain
import com.shielddns.app.domain.model.DomainCount
import com.shielddns.app.domain.repository.DailyBlockStats
import com.shielddns.app.domain.repository.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of StatsRepository using Room database.
 * Uses batching for efficient writes.
 */
@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val blockedQueryDao: BlockedQueryDao,
    private val dailyStatsDao: DailyStatsDao
) : StatsRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    // Buffering for batch inserts
    private val buffer = mutableListOf<BlockedQueryEntity>()
    private val bufferMutex = Mutex()
    private val bufferMaxSize = 50
    
    init {
        // Periodically flush buffer
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000) // Flush every 5 seconds
                flushBuffer()
            }
        }
    }

    override suspend fun recordBlockedQuery(domain: String) {
        val entity = BlockedQueryEntity(domain = domain)
        bufferMutex.withLock {
            buffer.add(entity)
            if (buffer.size >= bufferMaxSize) {
                flushBufferInternal()
            }
        }
    }

    override suspend fun recordBlockedQueries(domains: List<String>) {
        val entities = domains.map { BlockedQueryEntity(domain = it) }
        bufferMutex.withLock {
            buffer.addAll(entities)
            if (buffer.size >= bufferMaxSize) {
                flushBufferInternal()
            }
        }
    }

    private suspend fun flushBuffer() {
        bufferMutex.withLock {
            flushBufferInternal()
        }
    }

    private suspend fun flushBufferInternal() {
        if (buffer.isEmpty()) return
        
        val toFlush = buffer.toList()
        buffer.clear()
        
        blockedQueryDao.insertAll(toFlush)
        
        // Update daily stats
        updateDailyStats(toFlush)
    }

    private suspend fun updateDailyStats(queries: List<BlockedQueryEntity>) {
        val today = dateFormat.format(System.currentTimeMillis())
        val existing = dailyStatsDao.getStatsForDate(today)
        
        val dataSaved = queries.sumOf { it.estimatedSizeBytes.toLong() }
        val uniqueDomains = queries.map { it.domain }.distinct().size
        
        val updated = if (existing != null) {
            existing.copy(
                totalBlocked = existing.totalBlocked + queries.size,
                totalDataSavedBytes = existing.totalDataSavedBytes + dataSaved,
                uniqueDomains = existing.uniqueDomains + uniqueDomains,
                lastUpdated = System.currentTimeMillis()
            )
        } else {
            DailyStatsEntity(
                date = today,
                totalBlocked = queries.size.toLong(),
                totalDataSavedBytes = dataSaved,
                uniqueDomains = uniqueDomains
            )
        }
        
        dailyStatsDao.upsert(updated)
    }

    override fun getTodayStats(): Flow<BlockStats> {
        val startOfDay = getStartOfDay()
        return combine(
            blockedQueryDao.getBlockedCountToday(startOfDay),
            blockedQueryDao.getTotalBlockedCount(),
            blockedQueryDao.getTopBlockedDomains(startOfDay, 10),
            blockedQueryDao.getDataSavedBytes(startOfDay)
        ) { todayCount, totalCount, topDomains, dataSaved ->
            BlockStats(
                totalBlocked = totalCount,
                blockedToday = todayCount,
                topBlockedDomains = topDomains.map { DomainCount(it.domain, it.count) },
                dataSavedKb = (dataSaved ?: 0L) / 1024
            )
        }
    }

    override fun getBlockedCountToday(): Flow<Long> {
        return blockedQueryDao.getBlockedCountToday(getStartOfDay())
    }

    override fun getTotalBlocked(): Flow<Long> {
        return blockedQueryDao.getTotalBlockedCount()
    }

    override fun getTopBlockedDomains(limit: Int, sinceDays: Int): Flow<List<DomainCount>> {
        val since = getStartOfDayMinusDays(sinceDays)
        return blockedQueryDao.getTopBlockedDomains(since, limit).map { list ->
            list.map { DomainCount(it.domain, it.count) }
        }
    }

    override fun getDataSavedBytes(sinceDays: Int): Flow<Long> {
        val since = getStartOfDayMinusDays(sinceDays)
        return blockedQueryDao.getDataSavedBytes(since).map { it ?: 0L }
    }

    override fun getRecentBlocked(limit: Int): Flow<List<BlockedDomain>> {
        return blockedQueryDao.getBlockedToday(getStartOfDay(), limit).map { list ->
            list.map { entity ->
                BlockedDomain(
                    domain = entity.domain,
                    lastBlocked = entity.timestamp
                )
            }
        }
    }

    override fun getDailyStats(days: Int): Flow<List<DailyBlockStats>> {
        return dailyStatsDao.getRecentStats(days).map { list ->
            list.map { entity ->
                DailyBlockStats(
                    date = entity.date,
                    blockedCount = entity.totalBlocked,
                    dataSavedBytes = entity.totalDataSavedBytes
                )
            }
        }
    }

    override suspend fun cleanupOldData(olderThanDays: Int): Int {
        val cutoff = getStartOfDayMinusDays(olderThanDays)
        val cutoffDate = dateFormat.format(cutoff)
        
        val queriesDeleted = blockedQueryDao.deleteOlderThan(cutoff)
        dailyStatsDao.deleteOlderThan(cutoffDate)
        
        return queriesDeleted
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getStartOfDayMinusDays(days: Int): Long {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
