package com.example.browser.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.browser.data.dao.BookmarkDao
import com.example.browser.data.dao.HistoryDao
import com.example.browser.data.dao.TabDao
import com.example.browser.data.entity.BookmarkEntity
import com.example.browser.data.entity.HistoryEntity
import com.example.browser.data.entity.TabEntity

@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class, TabEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun tabDao(): TabDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "browser.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
