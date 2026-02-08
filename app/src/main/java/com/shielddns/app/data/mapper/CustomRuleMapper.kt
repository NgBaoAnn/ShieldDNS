package com.shielddns.app.data.mapper

import com.shielddns.app.data.local.database.entity.CustomRuleEntity
import com.shielddns.app.domain.model.CustomRule
import com.shielddns.app.domain.model.RuleType

/**
 * Mapper functions for converting between CustomRule domain model and CustomRuleEntity.
 */
object CustomRuleMapper {

    /**
     * Convert CustomRuleEntity to CustomRule domain model.
     */
    fun toDomain(entity: CustomRuleEntity): CustomRule {
        return CustomRule(
            id = entity.id,
            domain = entity.domain,
            ruleType = RuleType.valueOf(entity.ruleType),
            createdAt = entity.createdAt
        )
    }

    /**
     * Convert CustomRule domain model to CustomRuleEntity.
     */
    fun toEntity(rule: CustomRule): CustomRuleEntity {
        return CustomRuleEntity(
            id = rule.id,
            domain = rule.domain.lowercase(),
            ruleType = rule.ruleType.name,
            createdAt = rule.createdAt
        )
    }

    /**
     * Convert list of entities to domain models.
     */
    fun toDomainList(entities: List<CustomRuleEntity>): List<CustomRule> {
        return entities.map { toDomain(it) }
    }

    /**
     * Create a whitelist entity from domain string.
     */
    fun createWhitelistEntity(domain: String): CustomRuleEntity {
        return CustomRuleEntity(
            domain = domain.lowercase(),
            ruleType = RuleType.WHITELIST.name
        )
    }

    /**
     * Create a blacklist entity from domain string.
     */
    fun createBlacklistEntity(domain: String): CustomRuleEntity {
        return CustomRuleEntity(
            domain = domain.lowercase(),
            ruleType = RuleType.BLACKLIST.name
        )
    }
}
