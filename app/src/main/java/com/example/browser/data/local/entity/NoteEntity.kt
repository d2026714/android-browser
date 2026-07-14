package com.example.browser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val pageTitle: String,
    val noteType: String,        // "text" or "highlight"
    val content: String,         // text content or highlighted text
    val highlightColor: String?, // null for text notes; "yellow","green","blue","pink" for highlights
    val selectionStartOffset: Int? = null,  // character offset in page (for highlight positioning)
    val selectionEndOffset: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
