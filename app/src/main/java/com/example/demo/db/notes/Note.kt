package com.example.demo.db.notes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
internal data class Note(
    @PrimaryKey(autoGenerate = true)
    internal val id: Long = 0L,
    @ColumnInfo(name = "content")
    internal val content: String = "",
)
