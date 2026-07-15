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
        SettingsEntity::class,
        TabStateEntity::class,
        BookmarkFolderEntity::class,
        NoteEntity::class,
        DownloadEntity::class,
        NovelEntity::class,
        ChapterEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun readingListDao(): ReadingListDao
    abstract fun quickLinkDao(): QuickLinkDao
    abstract fun tabGroupDao(): TabGroupDao
    abstract fun settingsDao(): SettingsDao
    abstract fun tabStateDao(): TabStateDao
    abstract fun bookmarkFolderDao(): BookmarkFolderDao
    abstract fun noteDao(): NoteDao
    abstract fun downloadDao(): DownloadDao
    abstract fun novelDao(): NovelDao
    abstract fun chapterDao(): ChapterDao

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
