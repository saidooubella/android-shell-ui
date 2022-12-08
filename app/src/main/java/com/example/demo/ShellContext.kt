package com.example.demo

import android.app.Application
import java.io.File

internal interface ShellContext {
    val appContext: Application
    val repository: Repository
    val commands: CommandList
    var workingDir: File
    suspend fun sendEvent(event: Event)
}

internal sealed interface Event {
    object Clear : Event
    object Exit : Event
    data class Message(
        val content: String,
        val action: (suspend () -> Unit)? = null
    ) : Event
}
