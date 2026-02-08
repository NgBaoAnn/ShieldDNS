package com.shielddns.app.domain.repository

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
}
