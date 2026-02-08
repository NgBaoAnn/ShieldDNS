package com.shielddns.app.domain.model

/**
 * Represents a blocked domain entry.
 */
data class BlockedDomain(
    val domain: String,
    val source: BlockSource = BlockSource.DEFAULT_LIST,
    val blockedCount: Long = 0,
    val lastBlocked: Long? = null
) {
    enum class BlockSource {
        DEFAULT_LIST,   // From bundled blocklist
        USER_BLACKLIST, // User manually added
        REMOTE_LIST     // From remote sync
    }
}
