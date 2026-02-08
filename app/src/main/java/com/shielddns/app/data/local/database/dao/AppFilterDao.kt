package com.shielddns.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shielddns.app.data.local.database.entity.AppFilterEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for per-app DNS filter settings.
 */
@Dao
interface AppFilterDao {

    /**
     * Insert or update an app filter setting.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(appFilter: AppFilterEntity)

    /**
     * Insert multiple app filter settings.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(appFilters: List<AppFilterEntity>)

    /**
     * Update an existing app filter setting.
     */
    @Update
    suspend fun update(appFilter: AppFilterEntity)

    /**
     * Delete an app filter by package name.
     */
    @Query("DELETE FROM app_filters WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    /**
     * Get all app filter settings as a Flow.
     */
    @Query("SELECT * FROM app_filters ORDER BY appName ASC")
    fun getAllAppFilters(): Flow<List<AppFilterEntity>>

    /**
     * Get apps that are enabled for filtering.
     */
    @Query("SELECT * FROM app_filters WHERE isFiltered = 1 ORDER BY appName ASC")
    fun getFilteredApps(): Flow<List<AppFilterEntity>>

    /**
     * Get apps that are excluded from filtering.
     */
    @Query("SELECT * FROM app_filters WHERE isFiltered = 0 ORDER BY appName ASC")
    fun getExcludedApps(): Flow<List<AppFilterEntity>>

    /**
     * Get filter setting for a specific app.
     */
    @Query("SELECT * FROM app_filters WHERE packageName = :packageName")
    suspend fun getAppFilter(packageName: String): AppFilterEntity?

    /**
     * Check if an app is filtered.
     */
    @Query("SELECT isFiltered FROM app_filters WHERE packageName = :packageName")
    suspend fun isAppFiltered(packageName: String): Boolean?

    /**
     * Toggle filter state for an app.
     */
    @Query("UPDATE app_filters SET isFiltered = NOT isFiltered, updatedAt = :timestamp WHERE packageName = :packageName")
    suspend fun toggleFilter(packageName: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Get package names of filtered apps (for VPN builder).
     */
    @Query("SELECT packageName FROM app_filters WHERE isFiltered = 1")
    suspend fun getFilteredPackageNames(): List<String>

    /**
     * Get package names of excluded apps (for VPN builder).
     */
    @Query("SELECT packageName FROM app_filters WHERE isFiltered = 0")
    suspend fun getExcludedPackageNames(): List<String>

    /**
     * Get total count of saved app filters.
     */
    @Query("SELECT COUNT(*) FROM app_filters")
    fun getAppFilterCount(): Flow<Int>
}
