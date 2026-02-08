package com.shielddns.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shielddns.app.data.local.database.entity.BlockedQueryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for blocked queries operations.
 */
@Dao
interface BlockedQueryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(query: BlockedQueryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(queries: List<BlockedQueryEntity>)

    @Query("SELECT * FROM blocked_queries WHERE timestamp >= :startOfDay ORDER BY timestamp DESC LIMIT :limit")
    fun getBlockedToday(startOfDay: Long, limit: Int = 100): Flow<List<BlockedQueryEntity>>

    @Query("SELECT COUNT(*) FROM blocked_queries WHERE timestamp >= :startOfDay")
    fun getBlockedCountToday(startOfDay: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM blocked_queries")
    fun getTotalBlockedCount(): Flow<Long>

    @Query("""
        SELECT domain, COUNT(*) as count 
        FROM blocked_queries 
        WHERE timestamp >= :since
        GROUP BY domain 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    fun getTopBlockedDomains(since: Long, limit: Int = 10): Flow<List<DomainCountTuple>>

    @Query("SELECT SUM(estimatedSizeBytes) FROM blocked_queries WHERE timestamp >= :since")
    fun getDataSavedBytes(since: Long): Flow<Long?>

    @Query("DELETE FROM blocked_queries WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long): Int

    @Query("DELETE FROM blocked_queries")
    suspend fun deleteAll()
}

/**
 * Tuple for domain count aggregation queries.
 */
data class DomainCountTuple(
    val domain: String,
    val count: Long
)
