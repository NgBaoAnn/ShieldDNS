package com.shielddns.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing per-app DNS filter settings.
 */
@Entity(tableName = "app_filters")
data class AppFilterEntity(
    /**
     * Package name of the app (e.g., "com.google.android.youtube").
     */
    @PrimaryKey
    val packageName: String,

    /**
     * Display name of the app.
     */
    val appName: String,

    /**
     * Whether DNS filtering is enabled for this app.
     * true = filter this app's DNS, false = exclude from filtering.
     */
    val isFiltered: Boolean = true,

    /**
     * Timestamp when the setting was last updated (epoch millis).
     */
    val updatedAt: Long = System.currentTimeMillis()
)
