package com.shielddns.app.domain.repository

import com.shielddns.app.domain.model.CustomRule
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for blocklist operations.
 * Part of domain layer - defines contracts without implementation details.
 */
interface BlocklistRepository {
    
    /**
     * Load the default blocklist from bundled assets.
     * @return Set of blocked domain names
     */
    suspend fun loadDefaultBlocklist(): Set<String>
    
    /**
     * Get user-defined whitelist (domains to never block).
     */
    suspend fun getWhitelist(): Set<String>
    
    /**
     * Get user-defined blacklist (additional domains to block).
     */
    suspend fun getUserBlacklist(): Set<String>
    
    /**
     * Add a domain to the whitelist.
     * @param domain Domain to whitelist
     */
    suspend fun addToWhitelist(domain: String)
    
    /**
     * Remove a domain from the whitelist.
     * @param domain Domain to remove
     */
    suspend fun removeFromWhitelist(domain: String)
    
    /**
     * Add a domain to the user blacklist.
     * @param domain Domain to block
     */
    suspend fun addToBlacklist(domain: String)
    
    /**
     * Remove a domain from the user blacklist.
     * @param domain Domain to remove
     */
    suspend fun removeFromBlacklist(domain: String)
    
    /**
     * Get the total count of blocked domains (default + user blacklist).
     */
    suspend fun getBlockedDomainsCount(): Int

    // ========== Flow-based methods for reactive UI ==========

    /**
     * Get all custom rules as a reactive Flow.
     */
    fun observeAllCustomRules(): Flow<List<CustomRule>>

    /**
     * Get whitelist rules as a reactive Flow.
     */
    fun observeWhitelistRules(): Flow<List<CustomRule>>

    /**
     * Get blacklist rules as a reactive Flow.
     */
    fun observeBlacklistRules(): Flow<List<CustomRule>>

    /**
     * Get whitelist domains as a reactive Flow.
     */
    fun observeWhitelistDomains(): Flow<List<String>>

    /**
     * Get blacklist domains as a reactive Flow.
     */
    fun observeBlacklistDomains(): Flow<List<String>>

    /**
     * Remove a custom rule by domain.
     */
    suspend fun removeRule(domain: String)

    /**
     * Check if a domain exists in custom rules.
     */
    suspend fun ruleExists(domain: String): Boolean

    /**
     * Get count of custom rules as a Flow.
     */
    fun observeRuleCount(): Flow<Int>
}
