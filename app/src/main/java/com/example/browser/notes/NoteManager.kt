package com.example.browser.notes

import com.example.browser.data.local.dao.NoteDao
import com.example.browser.data.local.entity.NoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

enum class NoteType { TEXT, HIGHLIGHT }

enum class HighlightColor(val hex: String) {
    YELLOW("#FFF9C4"),
    GREEN("#C8E6C9"),
    BLUE("#BBDEFB"),
    PINK("#F8BBD0");

    companion object {
        fun fromName(name: String): HighlightColor {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: YELLOW
        }
    }
}

data class Note(
    val id: Long = 0,
    val url: String,
    val pageTitle: String,
    val noteType: NoteType,
    val content: String,
    val highlightColor: HighlightColor? = null,
    val selectionStartOffset: Int? = null,
    val selectionEndOffset: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class NoteManager(
    private val noteDao: NoteDao,
    private val scope: CoroutineScope
) {

    fun allNotesFlow(): Flow<List<NoteEntity>> = noteDao.getAllFlow()

    fun notesForUrlFlow(url: String): Flow<List<NoteEntity>> = noteDao.getByUrlFlow(url)

    suspend fun allNotes(): List<NoteEntity> = noteDao.getAll()

    suspend fun notesForUrl(url: String): List<NoteEntity> = noteDao.getByUrl(url)

    suspend fun search(query: String): List<NoteEntity> = noteDao.search(query)

    suspend fun noteCount(): Int = noteDao.count()

    fun addTextNote(url: String, pageTitle: String, content: String, onDone: ((Long) -> Unit)? = null) {
        scope.launch {
            val note = NoteEntity(
                url = url,
                pageTitle = pageTitle,
                noteType = NoteType.TEXT.name,
                content = content,
                highlightColor = null
            )
            val id = noteDao.insert(note)
            onDone?.invoke(id)
        }
    }

    fun addHighlight(
        url: String,
        pageTitle: String,
        highlightedText: String,
        color: HighlightColor = HighlightColor.YELLOW,
        startOffset: Int? = null,
        endOffset: Int? = null,
        onDone: ((Long) -> Unit)? = null
    ) {
        scope.launch {
            val note = NoteEntity(
                url = url,
                pageTitle = pageTitle,
                noteType = NoteType.HIGHLIGHT.name,
                content = highlightedText,
                highlightColor = color.name,
                selectionStartOffset = startOffset,
                selectionEndOffset = endOffset
            )
            val id = noteDao.insert(note)
            onDone?.invoke(id)
        }
    }

    fun updateNote(note: NoteEntity) {
        scope.launch {
            noteDao.update(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(id: Long) {
        scope.launch {
            noteDao.deleteById(id)
        }
    }

    fun deleteAllForUrl(url: String) {
        scope.launch {
            noteDao.deleteByUrl(url)
        }
    }

    suspend fun exportNotesAsMarkdown(): String {
        val notes = noteDao.getAll()
        if (notes.isEmpty()) return "# Browser Notes\n\nNo notes yet."

        val sb = StringBuilder()
        sb.appendLine("# Browser Notes")
        sb.appendLine()
        sb.appendLine("_Exported on ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}_")
        sb.appendLine()

        // Group by URL
        val grouped = notes.groupBy { it.url }
        for ((url, urlNotes) in grouped) {
            val pageTitle = urlNotes.firstOrNull()?.pageTitle ?: url
            sb.appendLine("## [$pageTitle]($url)")
            sb.appendLine()

            for (note in urlNotes) {
                when (note.noteType) {
                    NoteType.HIGHLIGHT.name -> {
                        val color = note.highlightColor ?: "YELLOW"
                        sb.appendLine("- 🖍️ **Highlight** ($color):")
                        sb.appendLine("  > ${note.content}")
                    }
                    else -> {
                        sb.appendLine("- 📝 **Note:**")
                        sb.appendLine("  ${note.content}")
                    }
                }
                sb.appendLine()
            }
            sb.appendLine("---")
            sb.appendLine()
        }

        return sb.toString()
    }
}
