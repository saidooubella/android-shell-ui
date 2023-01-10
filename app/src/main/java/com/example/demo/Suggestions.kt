package com.example.demo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal interface Suggestions {

    suspend fun supply(context: ShellContext, hint: String): List<Suggestion>

    data class Custom(val suggestions: List<Suggestion>) : Suggestions {
        override suspend fun supply(context: ShellContext, hint: String): List<Suggestion> {
            return suggestions.filter { hint in it.replacement }
        }
    }

    object Empty : Suggestions {
        override suspend fun supply(context: ShellContext, hint: String) = emptyList<Suggestion>()
    }

    object Commands : Suggestions {
        override suspend fun supply(context: ShellContext, hint: String): List<Suggestion> {
            return context.commands.map { Suggestion(it.name) }
        }
    }

    object Applications : Suggestions {
        override suspend fun supply(context: ShellContext, hint: String): List<Suggestion> {
            return context.repository
                .loadLauncherApps { it.contains(hint, true) }
                .map { Suggestion(it.name) }
        }
    }

    object Directories : Suggestions {
        override suspend fun supply(context: ShellContext, hint: String): List<Suggestion> {

            val index = hint.lastIndexOf(File.separator)
            val root = context.normalizePath(hint) {
                if (index != -1) hint.substring(0, index + 1) else ""
            }

            val query = if (index != -1) hint.substring(index + 1) else hint
            val specialPaths = buildList(3) {
                if (!hint.endsWith(File.separator))
                    add(Suggestion("/", "$hint/"))
                add(Suggestion(".", "$hint./"))
                add(Suggestion("..", "$hint../"))
            }

            return when (!root.exists()) {
                true -> emptyList()
                else -> withContext(Dispatchers.Default) {
                    specialPaths + context.repository.loadFiles(root, query).map {
                        Suggestion(it.name, context.removeWorkingDir(it.path) + File.separator)
                    }
                }
            }
        }
    }
}
