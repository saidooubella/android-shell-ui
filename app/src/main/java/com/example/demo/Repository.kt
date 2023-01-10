package com.example.demo

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.room.*
import com.example.demo.db.AppDatabase
import com.example.demo.db.notes.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class Repository(
    private val packageManager: PackageManager,
    private val appDatabase: AppDatabase,
) {

    suspend fun loadLauncherApps(predicate: (String) -> Boolean = { true }) = withContext(Dispatchers.IO) {
        packageManager.queryLauncherActivities()
            .filter { predicate(it.loadLabel(packageManager).toString()) }
            .map {
                AppModel(
                    it.loadLabel(packageManager).toString(),
                    it.activityInfo.packageName,
                    it.loadLaunchIntent(packageManager)
                )
            }
            .sortedBy { it.name }
    }

    suspend fun loadFiles(file: File, query: String = "") = withContext(Dispatchers.IO) {
        file.listFiles()?.filter { it.name.contains(query, true) }?.sortedBy { it.name } ?: listOf()
    }

    suspend fun addNote(note: Note) {
        appDatabase.noteDao.insertNote(note)
    }

    suspend fun notesList(): List<Note> {
        return appDatabase.noteDao.notesList()
    }

    suspend fun getNote(index: Long): Note? {
        return appDatabase.noteDao.getNote(index)
    }

    suspend fun removeNote(index: Long): Int {
        return appDatabase.noteDao.removeNote(index)
    }

    suspend fun clearNotes() {
        appDatabase.noteDao.clearNotes()
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
