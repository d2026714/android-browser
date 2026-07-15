package com.example.browser.data.local.dao

import androidx.room.*
import com.example.browser.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterIndex ASC")
    fun getChaptersByNovel(novelId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterIndex ASC")
    suspend fun getChaptersByNovelList(novelId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND chapterIndex = :index")
    suspend fun getChapter(novelId: Long, index: Int): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND chapterIndex = :index AND isCached = 1")
    suspend fun getCachedChapter(novelId: Long, index: Int): ChapterEntity?

    @Query("SELECT COUNT(*) FROM chapters WHERE novelId = :novelId")
    suspend fun getChapterCount(novelId: Long): Int

    @Query("SELECT COUNT(*) FROM chapters WHERE novelId = :novelId AND isCached = 1")
    suspend fun getCachedChapterCount(novelId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<ChapterEntity>)

    @Update
    suspend fun update(chapter: ChapterEntity)

    @Query("UPDATE chapters SET content = :content, isCached = 1 WHERE id = :id")
    suspend fun updateContent(id: Long, content: String)

    @Query("DELETE FROM chapters WHERE novelId = :novelId")
    suspend fun deleteByNovel(novelId: Long)

    @Query("DELETE FROM chapters WHERE novelId = :novelId AND isCached = 1")
    suspend fun clearCache(novelId: Long)
}
