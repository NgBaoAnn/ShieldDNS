package com.shielddns.app.domain.model

/**
 * Represents a DNS query intercepted by the VPN.
 */
data class DnsQuery(
    val transactionId: Int,
    val domain: String,
    val queryType: QueryType = QueryType.A,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class QueryType(val value: Int) {
        A(1),      // IPv4 address
        AAAA(28),  // IPv6 address
        CNAME(5),  // Canonical name
        MX(15),    // Mail exchange
        TXT(16),   // Text record
        UNKNOWN(0)
    }
}
