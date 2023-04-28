package com.saidooubella.shellui.data

import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.provider.ContactsContract.CommonDataKinds.Phone.*
import androidx.room.*
import com.saidooubella.shellui.commands.Arguments
import com.saidooubella.shellui.commands.Command
import com.saidooubella.shellui.commands.CommandList
import com.saidooubella.shellui.commands.CountCheckResult
import com.saidooubella.shellui.data.DataRepository.Companion.loadLaunchIntent
import com.saidooubella.shellui.data.DataRepository.Companion.queryLauncherActivities
import com.saidooubella.shellui.data.notes.Note
import com.saidooubella.shellui.data.pinned.PinnedApp
import com.saidooubella.shellui.data.pinned.PinnedAppsDao
import com.saidooubella.shellui.data.rss.RssFeed
import com.saidooubella.shellui.models.Contact
import com.saidooubella.shellui.models.LauncherApp
import com.saidooubella.shellui.models.parseArguments
import com.saidooubella.shellui.shell.Action
import com.saidooubella.shellui.suggestions.Suggestion
import com.saidooubella.shellui.utils.OpenableApp
import com.saidooubella.shellui.utils.catch
import com.saidooubella.shellui.utils.loadFromPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

internal interface UseCase<P, R> {
    suspend operator fun invoke(param: P): R
}

internal class LoadAppsUseCase(
    private val packageManager: PackageManager
) : UseCase<(String) -> Boolean, List<LauncherApp>> {

    override suspend fun invoke(param: (String) -> Boolean): List<LauncherApp> =
        withContext(Dispatchers.IO) {
            packageManager.queryLauncherActivities()
                .filter { param(it.loadLabel(packageManager).toString()) }
                .map {
                    LauncherApp(
                        it.loadLabel(packageManager).toString(),
                        it.activityInfo.packageName,
                        it.loadLaunchIntent(packageManager)
                    )
                }
                .sortedBy { it.name }
        }

    companion object {

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

internal class LoadContactsUseCase(
    private val resolver: ContentResolver,
) : UseCase<String, List<Contact>> {

    override suspend fun invoke(param: String): List<Contact> {
        return catch {
            resolver.query(CONTENT_URI, PROJECTION, SELECTION, arrayOf("%$param%"), SORT_ORDER)
                ?.use { cursor ->

                    val contactsList = ArrayList<Contact>(cursor.count)

                    val nameColumn = cursor.getColumnIndexOrThrow(DISPLAY_NAME_PRIMARY)
                    val phoneColumn = cursor.getColumnIndexOrThrow(NUMBER)

                    while (coroutineContext.isActive && cursor.moveToNext()) {
                        val phone: String = cursor.getString(phoneColumn)
                        val name: String = cursor.getString(nameColumn)
                        contactsList.add(Contact(name, phone))
                    }

                    contactsList
                }
        } ?: emptyList()
    }

    companion object {

        private const val IS_SDN_CONTACT = "is_sdn_contact"

        private val PROJECTION = arrayOf(DISPLAY_NAME_PRIMARY, NUMBER)

        private const val SELECTION =
            "$DISPLAY_NAME_PRIMARY LIKE ? AND $HAS_PHONE_NUMBER = 1 AND $IS_SDN_CONTACT = 0"

        private const val SORT_ORDER = "$DISPLAY_NAME_PRIMARY ASC"
    }
}

internal class LoadFilesUseCase : UseCase<LoadFilesUseCase.Params, List<File>> {

    override suspend fun invoke(param: Params): List<File> = withContext(Dispatchers.IO) {
        param.file.listFiles()?.filter {
            (if (param.dirsOnly) it.isDirectory else true) && it.name.contains(param.query, true)
        }?.sortedBy { it.name } ?: listOf()
    }

    internal class Params(
        internal val file: File,
        internal val query: String = "",
        internal val dirsOnly: Boolean = false
    )
}

internal class GetPinnedAppsSuggestion(
    private val pinnedAppsDao: PinnedAppsDao,
    private val packageManager: PackageManager,
) : UseCase<Unit, Flow<List<Suggestion>>> {
    override suspend fun invoke(param: Unit): Flow<List<Suggestion>> {
        return pinnedAppsDao.get().map { list ->
            list.mapNotNull { app ->
                catch {
                    val packageName = packageManager
                        .getLaunchIntentForPackage(app.packageName)
                        ?.component?.packageName ?: return@mapNotNull null
                    val label = packageManager.loadFromPackage(packageName)
                        .loadLabel(packageManager).toString()
                    OpenableApp(label, app.packageName)
                }
            }
        }
    }
}

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
        return catch {
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

                contactsList
            }
        } ?: emptyList()
    }

    suspend fun addNote(note: Note) {
        appDatabase.noteDao.insert(note)
    }

    suspend fun notesList(): List<Note> {
        return appDatabase.noteDao.list()
    }

    suspend fun getNote(index: Long): Note? {
        return appDatabase.noteDao.get(index)
    }

    suspend fun removeNote(index: Long): Int {
        return appDatabase.noteDao.remove(index)
    }

    suspend fun clearNotes() {
        appDatabase.noteDao.clear()
    }

    suspend fun pinApp(appPackage: String) {
        appDatabase.pinnedAppsDao.insert(PinnedApp(packageName = appPackage))
    }

    suspend fun unpinApp(appPackage: String): Boolean {
        return appDatabase.pinnedAppsDao.remove(appPackage) > 0
    }

    fun getPinnedApps(): Flow<List<PinnedApp>> {
        return appDatabase.pinnedAppsDao.get()
    }

    suspend fun getPinnedAppsList(): List<PinnedApp> {
        return appDatabase.pinnedAppsDao.getList()
    }

    suspend fun insertFeed(feed: RssFeed): Boolean {
        return appDatabase.rssFeedDao.insert(feed) > 0
    }

    suspend fun removeFeed(feedName: String): Boolean {
        return appDatabase.rssFeedDao.remove(feedName) > 0
    }

    suspend fun getFeeds(): List<RssFeed> {
        return appDatabase.rssFeedDao.getAll()
    }

    suspend fun getFeed(feedName: String): RssFeed? {
        return appDatabase.rssFeedDao.get(feedName)
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
