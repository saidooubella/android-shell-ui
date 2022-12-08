package com.example.demo

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Entity(tableName = "notes")
internal data class Note(
    @PrimaryKey(autoGenerate = true)
    internal val id: Long = 0L,
    @ColumnInfo(name = "content")
    internal val content: String = "",
)

@Dao
internal interface NoteDao {
    @Insert
    suspend fun insertNote(note: Note)
    @Query("SELECT * FROM notes")
    suspend fun notesList(): List<Note>
}

@Database(entities = [Note::class], version = 1)
internal abstract class AppDatabase : RoomDatabase() {

    abstract val noteDao: NoteDao

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

internal class Repository(
    private val packageManager: PackageManager,
    private val appDatabase: AppDatabase,
) {

    suspend fun loadLauncherApps() = withContext(Dispatchers.IO) {
        packageManager.queryLauncherActivities().sortedBy {
            it.loadLabel(packageManager).toString()
        }.map {
            AppModel(it.loadLabel(packageManager).toString(), it.loadLaunchIntent(packageManager))
        }
    }

    suspend fun loadFiles(file: File) = withContext(Dispatchers.IO) {
        file.list()?.toList() ?: listOf()
    }

    suspend fun addNote(note: Note) {
        appDatabase.noteDao.insertNote(note)
    }

    suspend fun notesList(): List<Note> {
        return appDatabase.noteDao.notesList()
    }

    companion object {

        private val FILTER_INTENT = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        @Suppress("DEPRECATION")
        private fun PackageManager.queryLauncherActivities(): MutableList<ResolveInfo> {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                queryIntentActivities(FILTER_INTENT, 0)
            } else {
                queryIntentActivities(FILTER_INTENT, PackageManager.ResolveInfoFlags.of(0))
            }
        }

        private fun ResolveInfo.loadLaunchIntent(packageManager: PackageManager): Intent {
            return Intent(packageManager.getLaunchIntentForPackage(activityInfo.packageName))
        }
    }
}
