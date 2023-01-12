package com.example.demo.data

import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.provider.ContactsContract.CommonDataKinds.Phone.*
import androidx.room.*
import com.example.demo.data.notes.Note
import com.example.demo.models.LauncherApp
import com.example.demo.models.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

internal class DataRepository(
    private val packageManager: PackageManager,
    private val appDatabase: ShellDatabase,
    private val resolver: ContentResolver,
) {

    suspend fun loadLauncherApps(
        predicate: (String) -> Boolean = { true }
    ) = withContext(Dispatchers.IO) {
        packageManager.queryLauncherActivities()
            .filter { predicate(it.loadLabel(packageManager).toString()) }
            .map {
                LauncherApp(
                    it.loadLabel(packageManager).toString(),
                    it.activityInfo.packageName,
                    it.loadLaunchIntent(packageManager)
                )
            }
            .sortedBy { it.name }
    }

    suspend fun loadFiles(
        file: File, query: String = "", dirsOnly: Boolean = false
    ) = withContext(Dispatchers.IO) {
        file.listFiles()?.filter {
            (if (dirsOnly) it.isDirectory else true) && it.name.contains(query, true)
        }?.sortedBy { it.name } ?: listOf()
    }

    suspend fun loadContacts(query: String = ""): List<Contact> {
        try {
            resolver.query(
                CONTENT_URI, PROJECTION, SELECTION, arrayOf("%$query%"), SORT_ORDER
            )?.use { cursor ->

                val contactsList = ArrayList<Contact>(cursor.count)

                val nameColumn = cursor.getColumnIndexOrThrow(DISPLAY_NAME_PRIMARY)
                val phoneColumn = cursor.getColumnIndexOrThrow(NUMBER)

                while (coroutineContext.isActive && cursor.moveToNext()) {
                    val phone: String = cursor.getString(phoneColumn)
                    val name: String = cursor.getString(nameColumn)
                    contactsList.add(Contact(name, phone))
                }

                return contactsList
            }
        } catch (_: Exception) {
        }
        return emptyList()
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

        private const val IS_SDN_CONTACT = "is_sdn_contact"

        private val PROJECTION = arrayOf(DISPLAY_NAME_PRIMARY, NUMBER)

        private const val SELECTION =
            "$DISPLAY_NAME_PRIMARY LIKE ? AND $HAS_PHONE_NUMBER = 1 AND $IS_SDN_CONTACT = 0"

        private const val SORT_ORDER = "$DISPLAY_NAME_PRIMARY ASC"

        private fun ResolveInfo.loadLaunchIntent(packageManager: PackageManager): Intent {
            return Intent(packageManager.getLaunchIntentForPackage(activityInfo.packageName))
        }

        private val FILTER_INTENT = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        @Suppress("DEPRECATION")
        private fun PackageManager.queryLauncherActivities(): MutableList<ResolveInfo> {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                queryIntentActivities(FILTER_INTENT, 0)
            } else {
                queryIntentActivities(FILTER_INTENT, PackageManager.ResolveInfoFlags.of(0))
            }
        }
    }
}

