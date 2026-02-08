package com.shielddns.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shielddns.app.data.local.database.entity.DailyStatsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for daily statistics operations.
 */
@Dao
interface DailyStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: DailyStatsEntity)

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getStatsForDate(date: String): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats WHERE date >= :startDate ORDER BY date DESC")
    fun getStatsFromDate(startDate: String): Flow<List<DailyStatsEntity>>

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT :days")
    fun getRecentStats(days: Int): Flow<List<DailyStatsEntity>>

    @Query("SELECT SUM(totalBlocked) FROM daily_stats")
    fun getTotalBlockedAllTime(): Flow<Long?>

    @Query("SELECT SUM(totalDataSavedBytes) FROM daily_stats")
    fun getTotalDataSavedAllTime(): Flow<Long?>

    @Query("DELETE FROM daily_stats WHERE date < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: String): Int
}
