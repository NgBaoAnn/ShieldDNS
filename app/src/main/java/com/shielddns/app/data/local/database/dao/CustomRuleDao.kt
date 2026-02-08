package com.shielddns.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shielddns.app.data.local.database.entity.CustomRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for custom domain rules (whitelist/blacklist).
 */
@Dao
interface CustomRuleDao {

    /**
     * Insert a new custom rule.
     * If domain already exists, replace the existing rule.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CustomRuleEntity): Long

    /**
     * Delete a custom rule.
     */
    @Delete
    suspend fun delete(rule: CustomRuleEntity)

    /**
     * Delete a rule by domain.
     */
    @Query("DELETE FROM custom_rules WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)

    /**
     * Get all custom rules as a Flow (reactive).
     */
    @Query("SELECT * FROM custom_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<CustomRuleEntity>>

    /**
     * Get all whitelisted domains.
     */
    @Query("SELECT domain FROM custom_rules WHERE ruleType = 'WHITELIST'")
    fun getWhitelistDomains(): Flow<List<String>>

    /**
     * Get all blacklisted domains.
     */
    @Query("SELECT domain FROM custom_rules WHERE ruleType = 'BLACKLIST'")
    fun getBlacklistDomains(): Flow<List<String>>

    /**
     * Get whitelist rules.
     */
    @Query("SELECT * FROM custom_rules WHERE ruleType = 'WHITELIST' ORDER BY createdAt DESC")
    fun getWhitelistRules(): Flow<List<CustomRuleEntity>>

    /**
     * Get blacklist rules.
     */
    @Query("SELECT * FROM custom_rules WHERE ruleType = 'BLACKLIST' ORDER BY createdAt DESC")
    fun getBlacklistRules(): Flow<List<CustomRuleEntity>>

    /**
     * Check if a domain exists in custom rules.
     */
    @Query("SELECT COUNT(*) > 0 FROM custom_rules WHERE domain = :domain")
    suspend fun exists(domain: String): Boolean

    /**
     * Get whitelist domains as a list (non-reactive, for initialization).
     */
    @Query("SELECT domain FROM custom_rules WHERE ruleType = 'WHITELIST'")
    suspend fun getWhitelistDomainsSync(): List<String>

    /**
     * Get blacklist domains as a list (non-reactive, for initialization).
     */
    @Query("SELECT domain FROM custom_rules WHERE ruleType = 'BLACKLIST'")
    suspend fun getBlacklistDomainsSync(): List<String>

    /**
     * Get total count of custom rules.
     */
    @Query("SELECT COUNT(*) FROM custom_rules")
    fun getRuleCount(): Flow<Int>
}
