package com.saidooubella.shellui.data

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.saidooubella.shellui.data.notes.Note
import com.saidooubella.shellui.data.notes.NoteDao
import com.saidooubella.shellui.data.pinned.PinnedApp
import com.saidooubella.shellui.data.pinned.PinnedAppsDao
import com.saidooubella.shellui.data.rss.RssFeed
import com.saidooubella.shellui.data.rss.RssFeedDao

@Database(
    entities = [Note::class, PinnedApp::class, RssFeed::class],
    exportSchema = false,
    version = 1,
)
internal abstract class ShellDatabase : RoomDatabase() {

    internal abstract val pinnedAppsDao: PinnedAppsDao
    internal abstract val rssFeedDao: RssFeedDao
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