package com.shielddns.app.service.filter

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Filters domains against blocklist/whitelist.
 * Uses efficient HashSet lookup with subdomain matching.
 */
@Singleton
class BlocklistFilter @Inject constructor(
    private val domainMatcher: DomainMatcher
) {
    // Default blocked domains (ads, trackers)
    private val defaultBlocklist = mutableSetOf<String>()
    
    // User whitelist (always allow)
    private val whitelist = mutableSetOf<String>()
    
    // User blacklist (additional blocks)
    private val userBlacklist = mutableSetOf<String>()

    init {
        // Initialize with common ad domains
        loadDefaultBlocklist()
    }

    /**
     * Check if a domain should be blocked.
     */
    fun shouldBlock(domain: String): Boolean {
        val normalizedDomain = domain.lowercase()

        // Check whitelist first (always takes priority)
        if (isWhitelisted(normalizedDomain)) {
            return false
        }

        // Check user blacklist
        if (isInList(normalizedDomain, userBlacklist)) {
            return true
        }

        // Check default blocklist
        return isInList(normalizedDomain, defaultBlocklist)
    }

    private fun isWhitelisted(domain: String): Boolean {
        return isInList(domain, whitelist)
    }

    private fun isInList(domain: String, list: Set<String>): Boolean {
        // Direct match
        if (list.contains(domain)) return true

        // Check if any pattern in the list matches
        for (pattern in list) {
            if (domainMatcher.matches(domain, pattern)) {
                return true
            }
        }

        return false
    }

    /**
     * Add domain to whitelist.
     */
    fun addToWhitelist(domain: String) {
        whitelist.add(domain.lowercase())
    }

    /**
     * Remove domain from whitelist.
     */
    fun removeFromWhitelist(domain: String) {
        whitelist.remove(domain.lowercase())
    }

    /**
     * Add domain to user blacklist.
     */
    fun addToBlacklist(domain: String) {
        userBlacklist.add(domain.lowercase())
    }

    /**
     * Remove domain from user blacklist.
     */
    fun removeFromBlacklist(domain: String) {
        userBlacklist.remove(domain.lowercase())
    }

    /**
     * Get all whitelisted domains.
     */
    fun getWhitelist(): Set<String> = whitelist.toSet()

    /**
     * Get user blacklist.
     */
    fun getUserBlacklist(): Set<String> = userBlacklist.toSet()

    /**
     * Load domains into the default blocklist.
     */
    fun loadBlocklist(domains: List<String>) {
        defaultBlocklist.addAll(domains.map { it.lowercase() })
    }

    /**
     * Clear all lists.
     */
    fun clear() {
        defaultBlocklist.clear()
        whitelist.clear()
        userBlacklist.clear()
        loadDefaultBlocklist()
    }

    private fun loadDefaultBlocklist() {
        // Common ad and tracking domains
        defaultBlocklist.addAll(listOf(
            // Google Ads
            "googlesyndication.com",
            "googleadservices.com",
            "doubleclick.net",
            "googleads.g.doubleclick.net",
            "pagead2.googlesyndication.com",
            "adservice.google.com",
            
            // Facebook Ads
            "facebook.com/tr",
            "pixel.facebook.com",
            "an.facebook.com",
            
            // General Ad Networks
            "adnxs.com",
            "adsrvr.org",
            "criteo.com",
            "outbrain.com",
            "taboola.com",
            "amazon-adsystem.com",
            
            // Trackers
            "google-analytics.com",
            "googletagmanager.com",
            "analytics.google.com",
            "hotjar.com",
            "mixpanel.com",
            "segment.io",
            
            // YouTube Ads (partial)
            "youtube.com/api/stats/ads",
            "youtube.com/pagead",
            "youtubei.googleapis.com/youtubei/v1/log_event",
            
            // Mobile Ad SDKs
            "ads.mopub.com",
            "sdk.tapjoy.com",
            "ads.inmobi.com",
            "ads.unity3d.com",
            "applovin.com",
            "vungle.com"
        ))
    }
}
