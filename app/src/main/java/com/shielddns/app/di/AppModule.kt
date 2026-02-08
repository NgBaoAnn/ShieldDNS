package com.shielddns.app.di

import android.content.Context
import com.shielddns.app.data.local.blocklist.AssetBlocklistLoader
import com.shielddns.app.data.local.database.AppDatabase
import com.shielddns.app.data.local.database.dao.BlockedQueryDao
import com.shielddns.app.data.local.database.dao.DailyStatsDao
import com.shielddns.app.data.local.datastore.SettingsDataStore
import com.shielddns.app.data.repository.BlocklistRepositoryImpl
import com.shielddns.app.data.repository.SettingsRepositoryImpl
import com.shielddns.app.data.repository.StatsRepositoryImpl
import com.shielddns.app.domain.repository.BlocklistRepository
import com.shielddns.app.domain.repository.SettingsRepository
import com.shielddns.app.domain.repository.StatsRepository
import com.shielddns.app.service.dns.DnsResolver
import com.shielddns.app.service.filter.BlocklistFilter
import com.shielddns.app.service.filter.DomainMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for application-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ==================== Database ====================

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideBlockedQueryDao(database: AppDatabase): BlockedQueryDao {
        return database.blockedQueryDao()
    }

    @Provides
    @Singleton
    fun provideDailyStatsDao(database: AppDatabase): DailyStatsDao {
        return database.dailyStatsDao()
    }

    // ==================== DataStore ====================

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): SettingsDataStore {
        return SettingsDataStore(context)
    }

    // ==================== Repositories ====================

    @Provides
    @Singleton
    fun provideStatsRepository(
        blockedQueryDao: BlockedQueryDao,
        dailyStatsDao: DailyStatsDao
    ): StatsRepository {
        return StatsRepositoryImpl(blockedQueryDao, dailyStatsDao)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        settingsDataStore: SettingsDataStore
    ): SettingsRepository {
        return SettingsRepositoryImpl(settingsDataStore)
    }

    @Provides
    @Singleton
    fun provideBlocklistRepository(
        assetBlocklistLoader: AssetBlocklistLoader
    ): BlocklistRepository {
        return BlocklistRepositoryImpl(assetBlocklistLoader)
    }

    // ==================== Filter & DNS ====================

    @Provides
    @Singleton
    fun provideDomainMatcher(): DomainMatcher {
        return DomainMatcher()
    }

    @Provides
    @Singleton
    fun provideAssetBlocklistLoader(
        @ApplicationContext context: Context
    ): AssetBlocklistLoader {
        return AssetBlocklistLoader(context)
    }

    @Provides
    @Singleton
    fun provideBlocklistFilter(
        domainMatcher: DomainMatcher,
        assetBlocklistLoader: AssetBlocklistLoader
    ): BlocklistFilter {
        return BlocklistFilter(domainMatcher, assetBlocklistLoader)
    }

    @Provides
    @Singleton
    fun provideDnsResolver(): DnsResolver {
        return DnsResolver()
    }
}
