package com.example.demo

import android.app.Application
import android.content.Intent
import android.os.Environment
import androidx.activity.result.ActivityResult
import com.example.demo.commands.CommandList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import java.io.File
import kotlin.coroutines.coroutineContext

internal abstract class ShellContext(
    val appContext: Application,
    val repository: Repository,
    val commands: CommandList,
) {

    internal var workingDir: File = Environment.getExternalStorageDirectory()

    internal abstract suspend fun <R> sendAction(action: Action<R>): R

    internal fun removeWorkingDir(path: String): String {
        val root = workingDir.path
        return if (path.startsWith(root)) path.substring(root.length + 1, path.length) else path
    }

    internal inline fun normalizePath(path: String, transformer: (String) -> String = { it }): File {
        return when (path.startsWith(File.separator)) {
            false -> File(workingDir, transformer(path))
            else -> File(transformer(path))
        }
    }
}

internal sealed interface Action<R> {

    suspend fun execute(state: MutableStateFlow<ScreenState>): R

    object Clear : Action<Unit> {
        override suspend fun execute(state: MutableStateFlow<ScreenState>) {
            state.update { it.copy(logs = persistentListOf()) }
        }
    }

    object Exit : Action<Unit> {
        override suspend fun execute(state: MutableStateFlow<ScreenState>) {
            state.update { it.copy(exit = true) }
        }
    }

    class StartIntentForResult(private val intent: Intent) : Action<ActivityResult> {
        override suspend fun execute(state: MutableStateFlow<ScreenState>): ActivityResult {
            val deferred = CompletableDeferred<ActivityResult>(coroutineContext.job)
            state.update { it.copy(intentForResult = IntentForResult(intent, deferred)) }
            return deferred.await()
        }
    }

    class Message(
        private val content: String,
        private val action: (suspend () -> Unit)? = null
    ) : Action<Unit> {
        override suspend fun execute(state: MutableStateFlow<ScreenState>) {
            state.update { it.copy(logs = it.logs.add(0, LogItem(content, action))) }
        }
    }

    class RequestPermissions(private val permissions: Array<String>) : Action<Boolean> {
        override suspend fun execute(state: MutableStateFlow<ScreenState>): Boolean {
            val deferred = CompletableDeferred<Boolean>(coroutineContext.job)
            state.update { it.copy(permissions = PermissionsHandler(permissions, deferred)) }
            return deferred.await()
        }
    }

    class StartIntent(private val intent: Intent) : Action<Unit> {
        override suspend fun execute(state: MutableStateFlow<ScreenState>) {
            state.update { it.copy(intent = intent) }
        }
    }

    class Prompt(private val hint: String) : Action<String> {
        override suspend fun execute(state: MutableStateFlow<ScreenState>): String {
            val deferred = CompletableDeferred<String>(coroutineContext.job)
            state.update { it.copy(mode = ShellMode.PromptMode(hint, deferred)) }
            return deferred.await()
        }
    }
}
