package com.shielddns.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Aggregated daily statistics for efficient queries.
 * Stores pre-calculated metrics to avoid expensive COUNT queries.
 */
@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey
    val date: String, // Format: yyyy-MM-dd
    val totalBlocked: Long = 0,
    val totalDataSavedBytes: Long = 0,
    val uniqueDomains: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
