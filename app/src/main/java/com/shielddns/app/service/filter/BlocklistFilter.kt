package com.shielddns.app.service.filter

import android.util.Log
import com.shielddns.app.data.local.blocklist.AssetBlocklistLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Filters domains against blocklist/whitelist.
 * Uses efficient HashSet lookup with subdomain matching.
 */
@Singleton
class BlocklistFilter @Inject constructor(
    private val domainMatcher: DomainMatcher,
    private val assetBlocklistLoader: AssetBlocklistLoader
) {
    companion object {
        private const val TAG = "BlocklistFilter"
    }

    // Default blocked domains (ads, trackers)
    private val defaultBlocklist = mutableSetOf<String>()
    
    // User whitelist (always allow)
    private val whitelist = mutableSetOf<String>()
    
    // User blacklist (additional blocks)
    private val userBlacklist = mutableSetOf<String>()

    // Initialization scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Track if asset blocklist has been loaded
    @Volatile
    private var isInitialized = false

    init {
        // Initialize with common ad domains (fallback)
        loadDefaultBlocklist()
        // Load extended blocklist from assets
        initializeAssetBlocklist()
    }

    /**
     * Initialize blocklist from asset file asynchronously.
     */
    private fun initializeAssetBlocklist() {
        scope.launch {
            try {
                val assetDomains = assetBlocklistLoader.loadBlocklist()
                defaultBlocklist.addAll(assetDomains)
                isInitialized = true
                Log.d(TAG, "Blocklist initialized with ${defaultBlocklist.size} total domains")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load asset blocklist", e)
                isInitialized = true // Mark as initialized anyway to use fallback
            }
        }
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
     * Get total blocklist size.
     */
    fun getBlocklistSize(): Int = defaultBlocklist.size

    /**
     * Check if blocklist is initialized.
     */
    fun isReady(): Boolean = isInitialized

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
        // Common ad and tracking domains (fallback if asset loading fails)
        defaultBlocklist.addAll(listOf(
            // Google Ads
            "googlesyndication.com",
            "googleadservices.com",
            "doubleclick.net",
            "googleads.g.doubleclick.net",
            "pagead2.googlesyndication.com",
            "adservice.google.com",
            
            // Facebook Ads
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
            
            // Mobile Ad SDKs
            "ads.mopub.com",
            "ads.inmobi.com",
            "ads.unity3d.com",
            "applovin.com",
            "vungle.com"
        ))
    }
}

