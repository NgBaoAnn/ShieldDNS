package com.shielddns.app.domain.model

/**
 * Domain model representing a user-defined custom rule.
 * Used for whitelist (always allow) or blacklist (always block) domains.
 */
data class CustomRule(
    /**
     * Unique identifier for the rule.
     */
    val id: Long = 0,

    /**
     * The domain to apply the rule to (e.g., "example.com").
     * Stored in lowercase, normalized format.
     */
    val domain: String,

    /**
     * Type of rule: WHITELIST or BLACKLIST.
     */
    val ruleType: RuleType,

    /**
     * Timestamp when the rule was created (epoch millis).
     */
    val createdAt: Long = System.currentTimeMillis()
)
