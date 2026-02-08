package com.shielddns.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.shielddns.app.data.local.database.dao.BlockedQueryDao
import com.shielddns.app.data.local.database.dao.DailyStatsDao
import com.shielddns.app.data.local.database.entity.BlockedQueryEntity
import com.shielddns.app.data.local.database.entity.DailyStatsEntity

/**
 * Room database for ShieldDNS application.
 */
@Database(
    entities = [
        BlockedQueryEntity::class,
        DailyStatsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun blockedQueryDao(): BlockedQueryDao
    abstract fun dailyStatsDao(): DailyStatsDao

    companion object {
        private const val DATABASE_NAME = "shielddns_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
