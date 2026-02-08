package com.shielddns.app.di

import android.content.Context
import com.shielddns.app.data.local.blocklist.AssetBlocklistLoader
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
