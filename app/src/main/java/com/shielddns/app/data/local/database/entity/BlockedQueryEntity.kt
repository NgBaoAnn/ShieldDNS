package com.shielddns.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing individual blocked DNS queries.
 */
@Entity(
    tableName = "blocked_queries",
    indices = [
        Index(value = ["domain"]),
        Index(value = ["timestamp"])
    ]
)
data class BlockedQueryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val timestamp: Long = System.currentTimeMillis(),
    val estimatedSizeBytes: Int = 5000 // Average ad request size ~5KB
)
