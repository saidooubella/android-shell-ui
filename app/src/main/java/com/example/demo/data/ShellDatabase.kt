package com.example.demo.data

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.demo.data.notes.Note
import com.example.demo.data.notes.NoteDao

@Database(entities = [Note::class], version = 1, exportSchema = false)
internal abstract class ShellDatabase : RoomDatabase() {

    internal abstract val noteDao: NoteDao

    companion object {

        @Volatile
        private var APP_DB: ShellDatabase? = null

        private val LOCK = Any()

        internal fun get(application: Application) = APP_DB ?: synchronized(LOCK) {
            APP_DB ?: Room.databaseBuilder(application, ShellDatabase::class.java, "shell")
                .build().also { APP_DB = it }
        }
    }
}