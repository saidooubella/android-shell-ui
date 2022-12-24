package com.example.demo

import android.app.Application
import android.content.Intent
import android.os.Environment
import java.io.File

internal data class ShellContext(
    val appContext: Application,
    val repository: Repository,
    val commands: CommandList,
    val prompt: suspend (String) -> String,
    val sendEvent: (Event) -> Unit,
) {
    var workingDir: File = Environment.getExternalStorageDirectory()
}

internal sealed interface Event {
    object Clear : Event
    object Exit : Event
    data class StartIntent(val intent: Intent) : Event
    data class Message(
        val content: String,
        val action: (suspend () -> Unit)? = null
    ) : Event
}
