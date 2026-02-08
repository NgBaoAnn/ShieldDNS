package com.shielddns.app.service.filter

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Matches domains against patterns (exact match, wildcard, subdomain).
 */
@Singleton
class DomainMatcher @Inject constructor() {

    /**
     * Check if a domain matches a pattern.
     * 
     * Patterns can be:
     * - Exact match: "ads.example.com"
     * - Wildcard: "*.example.com" (matches any subdomain)
     * - Base domain: "example.com" (matches example.com and all subdomains)
     */
    fun matches(domain: String, pattern: String): Boolean {
        val normalizedDomain = domain.lowercase().trim()
        val normalizedPattern = pattern.lowercase().trim()

        return when {
            // Exact match
            normalizedDomain == normalizedPattern -> true
            
            // Wildcard pattern (*.example.com)
            normalizedPattern.startsWith("*.") -> {
                val baseDomain = normalizedPattern.removePrefix("*.")
                normalizedDomain == baseDomain || 
                normalizedDomain.endsWith(".$baseDomain")
            }
            
            // Subdomain match (pattern without wildcard matches domain and subdomains)
            normalizedDomain.endsWith(".$normalizedPattern") -> true
            
            else -> false
        }
    }

    /**
     * Extract the base domain from a full domain.
     * e.g., "sub.ads.example.com" -> "example.com"
     */
    fun extractBaseDomain(domain: String): String {
        val parts = domain.split(".")
        return if (parts.size >= 2) {
            parts.takeLast(2).joinToString(".")
        } else {
            domain
        }
    }

    /**
     * Check if domain is a subdomain of another.
     */
    fun isSubdomainOf(domain: String, parent: String): Boolean {
        val normalizedDomain = domain.lowercase()
        val normalizedParent = parent.lowercase()
        
        return normalizedDomain == normalizedParent ||
               normalizedDomain.endsWith(".$normalizedParent")
    }
}
