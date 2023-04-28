package com.saidooubella.shellui.data.notes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface NoteDao {

    @Insert
    suspend fun insert(note: Note)

    @Query("SELECT * FROM notes")
    suspend fun list(): List<Note>

    @Query("SELECT * FROM notes ORDER BY id LIMIT 1 OFFSET :index")
    suspend fun get(index: Long): Note?

    @Query("DELETE FROM notes WHERE id IN ( SELECT id FROM notes ORDER BY id LIMIT 1 OFFSET :index )")
    suspend fun remove(index: Long): Int

    @Query("DELETE FROM notes")
    suspend fun clear()
}
