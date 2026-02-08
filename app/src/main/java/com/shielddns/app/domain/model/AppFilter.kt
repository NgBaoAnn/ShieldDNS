package com.shielddns.app.domain.model

/**
 * Per-app filtering configuration.
 */
data class AppFilter(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true // true = filter this app's DNS
)
