package com.example.demo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal interface Suggestions {

    suspend fun supply(context: ShellContext, hint: String): List<Suggestion>

    object Empty : Suggestions {
        override suspend fun supply(context: ShellContext, hint: String) = emptyList<Suggestion>()
    }

    object Commands : Suggestions {
        override suspend fun supply(context: ShellContext, hint: String): List<Suggestion> {
            return context.commands.map { Suggestion.of(it.name, it.name) }
        }
    }

    object Applications : Suggestions {
        override suspend fun supply(context: ShellContext, hint: String): List<Suggestion> {
            return context.repository.loadLauncherApps().map { Suggestion.of(it.name, it.name) }
        }
    }

    object Directories : Suggestions {
        override suspend fun supply(context: ShellContext, hint: String): List<Suggestion> {
            val index = hint.lastIndexOf(File.separatorChar)
            val root = if (!hint.startsWith(File.separatorChar)) {
                File(context.workingDir, (if (index != -1) hint.substring(0, index + 1) else ""))
            } else {
                File(if (index != -1) hint.substring(0, index + 1) else "")
            }
            val query = if (index != -1) hint.substring(index + 1) else hint
            return when (!root.exists()) {
                true -> emptyList()
                else -> withContext(Dispatchers.IO) {
                    context.repository.loadFiles(root)
                        .filter { it.contains(query, true) }
                        .map { Suggestion.of(it) }
                }
            }
        }
    }
}
