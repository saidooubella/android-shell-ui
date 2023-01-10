package com.example.demo.db

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.demo.db.notes.Note
import com.example.demo.db.notes.NoteDao

@Database(entities = [Note::class], version = 1, exportSchema = false)
internal abstract class AppDatabase : RoomDatabase() {

    internal abstract val noteDao: NoteDao

    companion object {

        @Volatile
        private var APP_DB: AppDatabase? = null

        private val LOCK = Any()

        internal fun get(application: Application) = APP_DB ?: synchronized(LOCK) {
            APP_DB ?: Room.databaseBuilder(application, AppDatabase::class.java, "shell")
                .build().also { APP_DB = it }
        }
    }
}