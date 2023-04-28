package com.saidooubella.shellui.suggestions

import com.saidooubella.shellui.shell.ShellContext
import com.saidooubella.shellui.utils.catch
import com.saidooubella.shellui.utils.loadFromPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal interface Suggestions {

    suspend fun load(context: ShellContext, hint: String): List<Suggestion>

    class Custom(val suggestions: List<Suggestion>) : Suggestions {
        override suspend fun load(context: ShellContext, hint: String): List<Suggestion> {
            return suggestions.filter { hint in it.replacement }
        }
    }

    object Empty : Suggestions {
        override suspend fun load(context: ShellContext, hint: String) = emptyList<Suggestion>()
    }

    object Commands : Suggestions {
        override suspend fun load(context: ShellContext, hint: String): List<Suggestion> {
            return context.commands.names()
                .filter { it.contains(hint, ignoreCase = true) }
                .map { Suggestion(it) }
        }
    }

    object PinnedApps : Suggestions {
        override suspend fun load(context: ShellContext, hint: String): List<Suggestion> {
            return context.repository.getPinnedAppsList().mapNotNull { app ->
                catch {
                    val manager = context.appContext.packageManager
                    Suggestion(manager.loadFromPackage(app).loadLabel(manager).toString())
                }
            }
        }
    }

    object NotPinnedApps : Suggestions {
        override suspend fun load(context: ShellContext, hint: String): List<Suggestion> {
            val pinnedApps = context.repository.getPinnedAppsList()
            return context.repository
                .loadLauncherApps { it.contains(hint, true) }
                .filter { app -> pinnedApps.none { app.packageName == it.packageName } }
                .map { Suggestion(it.name) }
        }
    }

    object Apps : Suggestions {
        override suspend fun load(context: ShellContext, hint: String): List<Suggestion> {
            return context.repository
                .loadLauncherApps { it.contains(hint, true) }
                .map { Suggestion(it.name) }
        }
    }

    object AppsPackages : Suggestions {
        override suspend fun load(context: ShellContext, hint: String): List<Suggestion> {
            return context.repository
                .loadLauncherApps { it.contains(hint, true) }
                .map { Suggestion("${it.name}(${it.packageName})", it.packageName) }
        }
    }

    object Files : Suggestions {
        override suspend fun load(context: ShellContext, hint: String): List<Suggestion> {
            return context.loadFiles(hint, false)
        }
    }

    object Directories : Suggestions {
        override suspend fun load(context: ShellContext, hint: String): List<Suggestion> {
            return context.loadFiles(hint, true)
        }
    }
}

private suspend fun ShellContext.loadFiles(
    hint: String,
    dirsOnly: Boolean
): List<Suggestion> {

    val index = hint.lastIndexOf(File.separator)
    val root = normalizePath(hint) {
        if (index != -1) hint.substring(0, index + 1) else ""
    }

    val query = if (index != -1) hint.substring(index + 1) else hint
    val specialPaths = buildList(3) {
        if (!hint.endsWith(File.separator))
            this.add(Suggestion("/", "$hint/"))
        this.add(Suggestion(".", "$hint./"))
        this.add(Suggestion("..", "$hint../"))
    }

    return when (!root.exists()) {
        true -> emptyList()
        else -> withContext(Dispatchers.Default) {
            specialPaths + repository.loadFiles(root, query, dirsOnly).map {
                val ending = if (it.isDirectory) File.separator else " "
                Suggestion(it.name, removeWorkingDir(it.path) + ending)
            }
        }
    }
}
