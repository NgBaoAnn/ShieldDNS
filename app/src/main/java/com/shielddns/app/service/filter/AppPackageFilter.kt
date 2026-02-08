package com.shielddns.app.service.filter

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Filters DNS queries based on package name (per-app filtering).
 */
@Singleton
class AppPackageFilter @Inject constructor() {

    // Apps to filter (empty = filter all apps)
    private val filteredApps = mutableSetOf<String>()
    
    // Apps to exclude from filtering
    private val excludedApps = mutableSetOf<String>()
    
    // Filter mode
    private var mode: FilterMode = FilterMode.ALL_APPS

    enum class FilterMode {
        ALL_APPS,           // Filter all apps
        SELECTED_APPS,      // Only filter selected apps
        EXCLUDE_APPS        // Filter all except excluded apps
    }

    /**
     * Set the filtering mode.
     */
    fun setMode(mode: FilterMode) {
        this.mode = mode
    }

    /**
     * Check if a package should have its DNS filtered.
     */
    fun shouldFilter(packageName: String): Boolean {
        return when (mode) {
            FilterMode.ALL_APPS -> true
            FilterMode.SELECTED_APPS -> filteredApps.contains(packageName)
            FilterMode.EXCLUDE_APPS -> !excludedApps.contains(packageName)
        }
    }

    /**
     * Add app to filtered list.
     */
    fun addToFiltered(packageName: String) {
        filteredApps.add(packageName)
    }

    /**
     * Remove app from filtered list.
     */
    fun removeFromFiltered(packageName: String) {
        filteredApps.remove(packageName)
    }

    /**
     * Add app to excluded list.
     */
    fun addToExcluded(packageName: String) {
        excludedApps.add(packageName)
    }

    /**
     * Remove app from excluded list.
     */
    fun removeFromExcluded(packageName: String) {
        excludedApps.remove(packageName)
    }

    /**
     * Get filtered apps.
     */
    fun getFilteredApps(): Set<String> = filteredApps.toSet()

    /**
     * Get excluded apps.
     */
    fun getExcludedApps(): Set<String> = excludedApps.toSet()
}
