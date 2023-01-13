package com.example.demo.shell

import android.app.Application
import android.os.Environment
import com.example.demo.commands.Command
import com.example.demo.commands.CommandList
import com.example.demo.commands.Commands
import com.example.demo.managers.FlashManager
import com.example.demo.data.DataRepository
import java.io.File

internal abstract class ShellContext(
    val appContext: Application,
    val repository: DataRepository,
    val commands: CommandList = Commands,
    val flashManager: FlashManager = FlashManager.of(appContext),
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

    internal fun onCleared() {
        flashManager.onCleared()
    }
}
