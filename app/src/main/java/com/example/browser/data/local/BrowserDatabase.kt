package com.example.browser.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.browser.data.local.dao.*
import com.example.browser.data.local.entity.*

@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        ReadingItemEntity::class,
        QuickLinkEntity::class,
        TabGroupEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun readingListDao(): ReadingListDao
    abstract fun quickLinkDao(): QuickLinkDao
    abstract fun tabGroupDao(): TabGroupDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getInstance(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "browser.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
