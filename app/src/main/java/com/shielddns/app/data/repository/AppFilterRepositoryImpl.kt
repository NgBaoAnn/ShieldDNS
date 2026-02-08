package com.shielddns.app.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.shielddns.app.data.local.database.dao.AppFilterDao
import com.shielddns.app.data.local.database.entity.AppFilterEntity
import com.shielddns.app.domain.model.InstalledApp
import com.shielddns.app.domain.repository.AppFilterRepository
import com.shielddns.app.service.filter.AppPackageFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AppFilterRepository.
 * Manages per-app DNS filter settings with Room persistence.
 */
@Singleton
class AppFilterRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appFilterDao: AppFilterDao,
    private val appPackageFilter: AppPackageFilter
) : AppFilterRepository {

    companion object {
        private const val TAG = "AppFilterRepository"
    }

    private val packageManager: PackageManager = context.packageManager
    
    // Cache filter mode in DataStore later, for now in memory
    private var filterMode: AppPackageFilter.FilterMode = AppPackageFilter.FilterMode.ALL_APPS

    override fun observeAllApps(): Flow<List<InstalledApp>> {
        return appFilterDao.getAllAppFilters().map { entities ->
            entities.map { entity ->
                InstalledApp(
                    packageName = entity.packageName,
                    appName = entity.appName,
                    icon = try {
                        packageManager.getApplicationIcon(entity.packageName)
                    } catch (e: PackageManager.NameNotFoundException) {
                        null
                    },
                    isFiltered = entity.isFiltered
                )
            }
        }
    }

    override fun observeFilteredApps(): Flow<List<InstalledApp>> {
        return appFilterDao.getFilteredApps().map { entities ->
            entities.map { entity ->
                InstalledApp(
                    packageName = entity.packageName,
                    appName = entity.appName,
                    icon = try {
                        packageManager.getApplicationIcon(entity.packageName)
                    } catch (e: PackageManager.NameNotFoundException) {
                        null
                    },
                    isFiltered = entity.isFiltered
                )
            }
        }
    }

    override fun observeExcludedApps(): Flow<List<InstalledApp>> {
        return appFilterDao.getExcludedApps().map { entities ->
            entities.map { entity ->
                InstalledApp(
                    packageName = entity.packageName,
                    appName = entity.appName,
                    icon = try {
                        packageManager.getApplicationIcon(entity.packageName)
                    } catch (e: PackageManager.NameNotFoundException) {
                        null
                    },
                    isFiltered = entity.isFiltered
                )
            }
        }
    }

    override suspend fun loadInstalledApps() {
        withContext(Dispatchers.IO) {
            try {
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

                val resolveInfoList = packageManager.queryIntentActivities(mainIntent, 0)
                val installedApps = resolveInfoList.mapNotNull { resolveInfo ->
                    val appInfo = resolveInfo.activityInfo?.applicationInfo ?: return@mapNotNull null
                    val packageName = appInfo.packageName
                    
                    // Skip system apps that shouldn't be filtered
                    if (isSystemAppToSkip(packageName)) {
                        return@mapNotNull null
                    }

                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    
                    AppFilterEntity(
                        packageName = packageName,
                        appName = appName,
                        isFiltered = true // Default to filtered
                    )
                }.distinctBy { it.packageName }

                appFilterDao.upsertAll(installedApps)
                Log.i(TAG, "Loaded ${installedApps.size} installed apps")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load installed apps", e)
            }
        }
    }

    override suspend fun toggleAppFilter(packageName: String) {
        appFilterDao.toggleFilter(packageName)
        syncToFilter(packageName)
        Log.d(TAG, "Toggled filter for: $packageName")
    }

    override suspend fun setAppFilter(packageName: String, isFiltered: Boolean) {
        val existing = appFilterDao.getAppFilter(packageName)
        if (existing != null) {
            appFilterDao.upsert(existing.copy(isFiltered = isFiltered, updatedAt = System.currentTimeMillis()))
        }
        syncToFilter(packageName)
        Log.d(TAG, "Set filter for $packageName: $isFiltered")
    }

    override suspend fun isAppFiltered(packageName: String): Boolean {
        return appFilterDao.isAppFiltered(packageName) ?: true
    }

    override suspend fun getExcludedPackageNames(): List<String> {
        return appFilterDao.getExcludedPackageNames()
    }

    override fun getFilterMode(): AppPackageFilter.FilterMode {
        return filterMode
    }

    override suspend fun setFilterMode(mode: AppPackageFilter.FilterMode) {
        filterMode = mode
        appPackageFilter.setMode(mode)
        Log.d(TAG, "Set filter mode: $mode")
    }

    override fun observeAppCount(): Flow<Int> {
        return appFilterDao.getAppFilterCount()
    }

    /**
     * Sync filter state to in-memory AppPackageFilter.
     */
    private suspend fun syncToFilter(packageName: String) {
        val isFiltered = appFilterDao.isAppFiltered(packageName) ?: true
        if (isFiltered) {
            appPackageFilter.addToFiltered(packageName)
            appPackageFilter.removeFromExcluded(packageName)
        } else {
            appPackageFilter.addToExcluded(packageName)
            appPackageFilter.removeFromFiltered(packageName)
        }
    }

    /**
     * Check if this is a system app that should be skipped.
     */
    private fun isSystemAppToSkip(packageName: String): Boolean {
        val skipPackages = setOf(
            "com.android.settings",
            "com.android.systemui",
            "com.android.phone",
            "com.android.documentsui",
            context.packageName // Skip our own app
        )
        return skipPackages.contains(packageName)
    }
}
