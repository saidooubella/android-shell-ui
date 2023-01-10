package com.example.demo.db.notes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.demo.db.notes.Note

@Dao
internal interface NoteDao {

    @Insert
    suspend fun insertNote(note: Note)

    @Query("SELECT * FROM notes")
    suspend fun notesList(): List<Note>

    @Query("SELECT * FROM notes ORDER BY id LIMIT 1 OFFSET :index")
    suspend fun getNote(index: Long): Note?

    @Query("DELETE FROM notes WHERE id IN ( SELECT id FROM notes ORDER BY id LIMIT 1 OFFSET :index )")
    suspend fun removeNote(index: Long): Int

    @Query("DELETE FROM notes")
    suspend fun clearNotes()
}
