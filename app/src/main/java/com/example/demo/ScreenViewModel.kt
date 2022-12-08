package com.example.demo

import android.app.Application
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal class ScreenViewModel(
    private val repository: Repository,
    context: Application,
) : ViewModel() {

    private val commands: CommandList =
        CommandList.Builder()
            .putCommand(Command.Leaf(
                Metadata.Builder("echo")
                    .addRequiredNArgs("args", Suggestions.Empty)
                    .build()
            ) { arguments ->
                sendEvent(Event.Message(arguments.joinToString(" ") { it.value }))
            })
            .putCommand(Command.Leaf(Metadata.Builder("clear").build()) {
                sendEvent(Event.Clear)
            })
            .putCommand(Command.Leaf(Metadata.Builder("commands").build()) {
                commands.forEach { sendEvent(Event.Message(it.name)) }
            })
            .putCommand(Command.Leaf(Metadata.Builder("apps").build()) {
                repository.loadLauncherApps().forEach {
                    sendEvent(Event.Message(it.name) {
                        appContext.startActivity(it.intent)
                    })
                }
            })
            .putCommand(Command.Leaf(Metadata.Builder("pwd").build()) {
                sendEvent(Event.Message(workingDir.canonicalPath))
            })
            .putCommand(Command.Leaf(Metadata.Builder("ls").build()) {
                repository.loadFiles(workingDir).forEach {
                    sendEvent(Event.Message(it))
                }
            })
            .putCommand(Command.Leaf(
                Metadata.Builder("mkdir")
                    .addRequiredArg("folderName", Suggestions.Directories)
                    .build()
            ) { arguments ->
                val fileName = arguments[0].value
                val file = if (fileName.startsWith('/'))
                    File(fileName) else File(workingDir, fileName)
                when {
                    file.exists() -> sendEvent(Event.Message("$fileName already exists"))
                    !file.mkdirs() -> sendEvent(Event.Message("failed to create folder"))
                }
            })
            .putCommand(Command.Leaf(
                Metadata.Builder("cd")
                    .addRequiredArg("directory", Suggestions.Directories)
                    .build()
            ) { arguments ->
                val file = File(workingDir, arguments[0].value)
                if (file.exists()) {
                    workingDir = file
                } else {
                    sendEvent(Event.Message("${arguments[0]}: not found"))
                }
            })
            .putCommand(Command.Leaf(Metadata.Builder("exit").build()) {
                sendEvent(Event.Exit)
            })
            .putCommand(Command.Group("notes",
                CommandList.Builder().putCommand(Command.Leaf(Metadata.Builder("add").build()) {
                    repository.addNote(Note(content = it[0].value))
                }).putCommand(Command.Leaf(Metadata.Builder("list").build()) {
                    repository.notesList().forEachIndexed { index, note ->
                        sendEvent(Event.Message("$index: ${note.content}"))
                    }
                }).build()))
            .build()

    private var suggestionsJob: Job? = null
    private var execJob: Job? = null

    internal var state by mutableStateOf(ScreenState())
        private set

    private val context = object : ShellContext {

        override var workingDir: File = Environment.getExternalStorageDirectory()

        override val appContext: Application
            get() = context

        override val repository: Repository
            get() = this@ScreenViewModel.repository

        override val commands: CommandList
            get() = this@ScreenViewModel.commands

        override suspend fun sendEvent(event: Event): Unit = withContext(Dispatchers.Main) {
            state = when (event) {
                is Event.Clear -> state.copy(logs = persistentListOf())
                is Event.Message -> state.copy(logs = state.logs.add(0,
                    LogItem(event.content, event.action)))
                is Event.Exit -> state.copy(exit = true)
            }
        }
    }

    init {
        generateSuggestions()
    }

    internal fun submitLine() {
        val content = state.fieldText.text.trim()
        state = state.copy(fieldText = TextFieldValue(""))
        generateSuggestions()
        if (content.isBlank()) return
        execJob?.cancel()
        execJob = viewModelScope.launch {
            state = state.copy(isIdle = false)
            try {
                context.sendEvent(Event.Message(">> $content"))
                exec(content.toArguments(), commands)
            } finally {
                state = state.copy(isIdle = true)
            }
        }
    }

    internal fun changeFieldText(content: TextFieldValue) {
        state = state.copy(fieldText = content.copy(content.text.replace('\n', ' ')))
        generateSuggestions()
    }

    private fun generateSuggestions() {
        suggestionsJob?.cancel()
        suggestionsJob = viewModelScope.launch {
            val arguments = state.fieldText.text.toArguments()
            val suggestions = SuggestionsGenerator(context)
                .suggestions(arguments, state.fieldText.text.length)
            state = state.copy(suggestions = suggestions)
        }
    }

    internal fun toggleTheme() {
        state = state.copy(isDark = !state.isDark)
    }

    private suspend fun exec(arguments: Arguments, commands: CommandList) {
        when (arguments.isEmpty()) {
            true -> context.sendEvent(Event.Message("Too few arguments"))
            else -> when (val command = commands[arguments[0].value]) {
                is Command.Group -> exec(arguments.dropFirst(), command.commands)
                is Command.Leaf -> arguments.dropFirst().let { leafArgs ->
                    when (command.metadata.validateCount(leafArgs.count())) {
                        TooManyArgs -> context.sendEvent(Event.Message("Too many arguments"))
                        TooFewArgs -> context.sendEvent(Event.Message("Too few arguments"))
                        ExactArgs -> command.action(context, leafArgs)
                    }
                }
                null -> context.sendEvent(Event.Message("command not found ${arguments[0].value}"))
            }
        }
    }

    override fun onCleared() {
        suggestionsJob?.cancel()
        execJob?.cancel()
    }

    internal fun finishExiting() {
        state = state.copy(exit = false)
    }

    internal class Factory(
        private val application: Application,
        private val repository: Repository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScreenViewModel(repository, application) as T
        }
    }
}

data class Argument(val value: String, val start: Int, val end: Int) {
    override fun toString(): String = value
}

private fun String.isSpaceAt(i: Int): Boolean {
    return i < length && this[i].isWhitespace()
}

private fun String.toArguments(): Arguments {

    val arguments = mutableListOf<Argument>()
    val builder = StringBuilder()
    var index = 0

    while (index < length) {

        while (isSpaceAt(index)) {
            index += 1
        }

        var escape = false
        var quote = false
        val start = index

        builder.delete(0, builder.length)

        while (index < length) {

            val c = this[index]

            if (escape) {
                escape = false
                builder.append(c)
                index++
                continue
            }

            if (!quote && c.isWhitespace()) {
                break
            }

            when (c) {
                '\\' -> escape = true
                '"' -> quote = !quote
                else -> builder.append(c)
            }

            index += 1
        }

        if (start >= index) {
            continue
        }

        arguments.add(Argument(builder.toString(), start, index))
    }

    return Arguments(arguments.toList())
}
