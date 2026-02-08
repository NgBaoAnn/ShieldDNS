package com.shielddns.app.data.repository

import android.util.Log
import com.shielddns.app.data.local.blocklist.AssetBlocklistLoader
import com.shielddns.app.data.local.database.dao.CustomRuleDao
import com.shielddns.app.data.mapper.CustomRuleMapper
import com.shielddns.app.domain.model.CustomRule
import com.shielddns.app.domain.repository.BlocklistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of BlocklistRepository.
 * Loads blocklist from assets and manages user custom rules with Room persistence.
 */
@Singleton
class BlocklistRepositoryImpl @Inject constructor(
    private val assetBlocklistLoader: AssetBlocklistLoader,
    private val customRuleDao: CustomRuleDao
) : BlocklistRepository {

    companion object {
        private const val TAG = "BlocklistRepository"
    }

    private val mutex = Mutex()
    
    // Cached blocklist from assets
    private var cachedBlocklist: Set<String>? = null

    // ========== Legacy methods (backward compatibility) ==========

    override suspend fun loadDefaultBlocklist(): Set<String> = mutex.withLock {
        cachedBlocklist?.let { return@withLock it }
        
        val blocklist = assetBlocklistLoader.loadBlocklist()
        cachedBlocklist = blocklist
        Log.i(TAG, "Cached ${blocklist.size} domains from assets")
        blocklist
    }

    override suspend fun getWhitelist(): Set<String> {
        return customRuleDao.getWhitelistDomainsSync().toSet()
    }

    override suspend fun getUserBlacklist(): Set<String> {
        return customRuleDao.getBlacklistDomainsSync().toSet()
    }

    override suspend fun addToWhitelist(domain: String) {
        val normalized = domain.lowercase().trim()
        val entity = CustomRuleMapper.createWhitelistEntity(normalized)
        customRuleDao.insert(entity)
        Log.d(TAG, "Added to whitelist: $normalized")
    }

    override suspend fun removeFromWhitelist(domain: String) {
        val normalized = domain.lowercase().trim()
        customRuleDao.deleteByDomain(normalized)
        Log.d(TAG, "Removed from whitelist: $normalized")
    }

    override suspend fun addToBlacklist(domain: String) {
        val normalized = domain.lowercase().trim()
        val entity = CustomRuleMapper.createBlacklistEntity(normalized)
        customRuleDao.insert(entity)
        Log.d(TAG, "Added to blacklist: $normalized")
    }

    override suspend fun removeFromBlacklist(domain: String) {
        val normalized = domain.lowercase().trim()
        customRuleDao.deleteByDomain(normalized)
        Log.d(TAG, "Removed from blacklist: $normalized")
    }

    override suspend fun getBlockedDomainsCount(): Int {
        val defaultCount = loadDefaultBlocklist().size
        val userCount = customRuleDao.getBlacklistDomainsSync().size
        return defaultCount + userCount
    }

    // ========== Flow-based methods for reactive UI ==========

    override fun observeAllCustomRules(): Flow<List<CustomRule>> {
        return customRuleDao.getAllRules().map { entities ->
            CustomRuleMapper.toDomainList(entities)
        }
    }

    override fun observeWhitelistRules(): Flow<List<CustomRule>> {
        return customRuleDao.getWhitelistRules().map { entities ->
            CustomRuleMapper.toDomainList(entities)
        }
    }

    override fun observeBlacklistRules(): Flow<List<CustomRule>> {
        return customRuleDao.getBlacklistRules().map { entities ->
            CustomRuleMapper.toDomainList(entities)
        }
    }

    override fun observeWhitelistDomains(): Flow<List<String>> {
        return customRuleDao.getWhitelistDomains()
    }

    override fun observeBlacklistDomains(): Flow<List<String>> {
        return customRuleDao.getBlacklistDomains()
    }

    override suspend fun removeRule(domain: String) {
        val normalized = domain.lowercase().trim()
        customRuleDao.deleteByDomain(normalized)
        Log.d(TAG, "Removed rule: $normalized")
    }

    override suspend fun ruleExists(domain: String): Boolean {
        val normalized = domain.lowercase().trim()
        return customRuleDao.exists(normalized)
    }

    override fun observeRuleCount(): Flow<Int> {
        return customRuleDao.getRuleCount()
    }

    // ========== Additional utility methods ==========

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
     * Clear all user-defined rules from database.
     */
    suspend fun clearUserRules() {
        // Get all rules and delete them
        val whitelist = customRuleDao.getWhitelistDomainsSync()
        val blacklist = customRuleDao.getBlacklistDomainsSync()
        
        whitelist.forEach { customRuleDao.deleteByDomain(it) }
        blacklist.forEach { customRuleDao.deleteByDomain(it) }
        
        Log.i(TAG, "Cleared all user rules")
    }
}

