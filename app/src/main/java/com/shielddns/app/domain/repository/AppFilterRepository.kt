package com.shielddns.app.domain.repository

import com.shielddns.app.domain.model.InstalledApp
import com.shielddns.app.service.filter.AppPackageFilter
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for per-app DNS filter settings.
 * Part of domain layer - defines contracts without implementation details.
 */
interface AppFilterRepository {

    /**
     * Get all installed apps with their filter settings as a reactive Flow.
     */
    fun observeAllApps(): Flow<List<InstalledApp>>

    /**
     * Get apps that are enabled for DNS filtering.
     */
    fun observeFilteredApps(): Flow<List<InstalledApp>>

    /**
     * Get apps that are excluded from DNS filtering.
     */
    fun observeExcludedApps(): Flow<List<InstalledApp>>

    /**
     * Load all installed apps on the device.
     * This should be called initially to populate the database.
     */
    suspend fun loadInstalledApps()

    /**
     * Toggle DNS filter for a specific app.
     */
    suspend fun toggleAppFilter(packageName: String)

    /**
     * Set filter state for a specific app.
     */
    suspend fun setAppFilter(packageName: String, isFiltered: Boolean)

    /**
     * Get filter state for a specific app.
     * Returns true by default if not set.
     */
    suspend fun isAppFiltered(packageName: String): Boolean

    /**
     * Get package names of apps that should be excluded from VPN.
     * Used for VpnService.Builder.addDisallowedApplication().
     */
    suspend fun getExcludedPackageNames(): List<String>

    /**
     * Get current filter mode.
     */
    fun getFilterMode(): AppPackageFilter.FilterMode

    /**
     * Set filter mode (ALL_APPS, SELECTED_APPS, EXCLUDE_APPS).
     */
    suspend fun setFilterMode(mode: AppPackageFilter.FilterMode)

    /**
     * Get count of saved app filter settings.
     */
    fun observeAppCount(): Flow<Int>
}
