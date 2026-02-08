package com.shielddns.app.data.repository

import android.util.Log
import com.shielddns.app.data.local.blocklist.AssetBlocklistLoader
import com.shielddns.app.domain.repository.BlocklistRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of BlocklistRepository.
 * Loads blocklist from assets and manages user custom rules.
 */
@Singleton
class BlocklistRepositoryImpl @Inject constructor(
    private val assetBlocklistLoader: AssetBlocklistLoader
) : BlocklistRepository {

    companion object {
        private const val TAG = "BlocklistRepository"
    }

    private val mutex = Mutex()
    
    // Cached blocklist from assets
    private var cachedBlocklist: Set<String>? = null
    
    // User-defined rules (in-memory for now, could persist to Room/DataStore)
    private val whitelist = mutableSetOf<String>()
    private val userBlacklist = mutableSetOf<String>()

    override suspend fun loadDefaultBlocklist(): Set<String> = mutex.withLock {
        cachedBlocklist?.let { return@withLock it }
        
        val blocklist = assetBlocklistLoader.loadBlocklist()
        cachedBlocklist = blocklist
        Log.i(TAG, "Cached ${blocklist.size} domains from assets")
        blocklist
    }

    override suspend fun getWhitelist(): Set<String> = mutex.withLock {
        whitelist.toSet()
    }

    override suspend fun getUserBlacklist(): Set<String> = mutex.withLock {
        userBlacklist.toSet()
    }

    override suspend fun addToWhitelist(domain: String) {
        mutex.withLock {
            val normalized = domain.lowercase().trim()
            whitelist.add(normalized)
            Log.d(TAG, "Added to whitelist: $normalized")
        }
    }

    override suspend fun removeFromWhitelist(domain: String) {
        mutex.withLock {
            val normalized = domain.lowercase().trim()
            whitelist.remove(normalized)
            Log.d(TAG, "Removed from whitelist: $normalized")
        }
    }

    override suspend fun addToBlacklist(domain: String) {
        mutex.withLock {
            val normalized = domain.lowercase().trim()
            userBlacklist.add(normalized)
            Log.d(TAG, "Added to blacklist: $normalized")
        }
    }

    override suspend fun removeFromBlacklist(domain: String) {
        mutex.withLock {
            val normalized = domain.lowercase().trim()
            userBlacklist.remove(normalized)
            Log.d(TAG, "Removed from blacklist: $normalized")
        }
    }

    override suspend fun getBlockedDomainsCount(): Int {
        val defaultCount = loadDefaultBlocklist().size
        val userCount = mutex.withLock { userBlacklist.size }
        return defaultCount + userCount
    }

    /**
     * Force reload blocklist from assets (useful after app update).
     */
    suspend fun reloadBlocklist(): Set<String> = mutex.withLock {
        cachedBlocklist = null
        val blocklist = assetBlocklistLoader.loadBlocklist()
        cachedBlocklist = blocklist
        Log.i(TAG, "Reloaded ${blocklist.size} domains")
        blocklist
    }

    /**
     * Clear all user-defined rules.
     */
    suspend fun clearUserRules() = mutex.withLock {
        whitelist.clear()
        userBlacklist.clear()
        Log.i(TAG, "Cleared all user rules")
    }
}
