package com.shielddns.app.data.local.blocklist

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads blocklist domains from assets/blocklist/hosts.txt.
 * 
 * Supports hosts file format:
 * - Lines starting with # are comments
 * - Format: 0.0.0.0 domain.com or 127.0.0.1 domain.com
 * - Empty lines are ignored
 */
@Singleton
class AssetBlocklistLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AssetBlocklistLoader"
        private const val BLOCKLIST_PATH = "blocklist/hosts.txt"
    }

    /**
     * Load domains from the hosts.txt asset file.
     * 
     * @return Set of blocked domain names
     */
    suspend fun loadBlocklist(): Set<String> = withContext(Dispatchers.IO) {
        val domains = mutableSetOf<String>()
        
        try {
            context.assets.open(BLOCKLIST_PATH).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    parseLine(line)?.let { domain ->
                        domains.add(domain)
                    }
                }
            }
            Log.d(TAG, "Loaded ${domains.size} domains from blocklist")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load blocklist", e)
        }
        
        domains
    }

    /**
     * Parse a single line from hosts file.
     * 
     * @return Domain name if valid, null otherwise
     */
    private fun parseLine(line: String): String? {
        val trimmed = line.trim()
        
        // Skip empty lines and comments
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null
        }
        
        // Split by whitespace
        val parts = trimmed.split(Regex("\\s+"))
        if (parts.size < 2) return null
        
        val ip = parts[0]
        val domain = parts[1].lowercase()
        
        // Validate IP is 0.0.0.0 or 127.0.0.1 (hosts file format)
        if (ip != "0.0.0.0" && ip != "127.0.0.1") {
            return null
        }
        
        // Skip localhost entries
        if (domain == "localhost" || domain == "local" || domain.isEmpty()) {
            return null
        }
        
        // Validate domain format (basic check)
        if (!domain.contains('.') || domain.startsWith('.') || domain.endsWith('.')) {
            return null
        }
        
        return domain
    }

    /**
     * Check if blocklist asset exists.
     */
    fun hasBlocklist(): Boolean {
        return try {
            context.assets.open(BLOCKLIST_PATH).close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
