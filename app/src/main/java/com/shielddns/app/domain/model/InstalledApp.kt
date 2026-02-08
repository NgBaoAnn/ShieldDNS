package com.shielddns.app.domain.model

import android.graphics.drawable.Drawable

/**
 * Domain model representing an installed app for per-app filtering.
 */
data class InstalledApp(
    /**
     * Package name of the app (e.g., "com.google.android.youtube").
     */
    val packageName: String,

    /**
     * Display name of the app.
     */
    val appName: String,

    /**
     * App icon drawable (optional, may be null if not loaded).
     */
    val icon: Drawable? = null,

    /**
     * Whether DNS filtering is enabled for this app.
     */
    val isFiltered: Boolean = true
)
