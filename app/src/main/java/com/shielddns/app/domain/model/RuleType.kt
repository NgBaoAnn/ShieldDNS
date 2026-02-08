package com.shielddns.app.domain.model

/**
 * Type of custom domain rule.
 */
enum class RuleType {
    /**
     * Whitelisted domains are never blocked, even if they appear in blocklists.
     */
    WHITELIST,

    /**
     * Blacklisted domains are always blocked, in addition to default blocklists.
     */
    BLACKLIST
}
