package com.shielddns.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing custom domain rules (whitelist/blacklist).
 */
@Entity(
    tableName = "custom_rules",
    indices = [Index(value = ["domain"], unique = true)]
)
data class CustomRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Domain name (e.g., "ads.example.com").
     * Stored in lowercase for consistent matching.
     */
    val domain: String,

    /**
     * Rule type: "WHITELIST" or "BLACKLIST".
     */
    val ruleType: String,

    /**
     * Timestamp when the rule was created (epoch millis).
     */
    val createdAt: Long = System.currentTimeMillis()
)
