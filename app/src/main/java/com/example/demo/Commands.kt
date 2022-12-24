package com.example.demo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import java.io.File

private val NOTE_COMMAND_GROUP = object : Command.Group("notes") {

    private val ADD_OPTION = Leaf(Metadata.Builder("add")
        .addRequiredArg("note", Suggestions.Empty)
        .build()) {
        repository.addNote(Note(content = it[0].value))
    }

    private val LIST_OPTION = Leaf(Metadata.Builder("list").build()) {
        repository.notesList().forEachIndexed { index, note ->
            sendEvent(Event.Message("$index: ${note.content}"))
        }
    }

    private val REMOVE_OPTION = Leaf(
        Metadata.Builder("rm")
            .addRequiredArg("index", Suggestions.Empty)
            .build()
    ) {
        it[0].value.toLongOrNull()?.let { index ->
            if (repository.removeNote(index) > 0) index else null
        } ?: run {
            sendEvent(Event.Message("Invalid index"))
        }
    }

    private val CLEAR_OPTION = Leaf(Metadata.Builder("clear").build()) {
        repository.clearNotes()
    }

    private val COPY_OPTION = Leaf(
        Metadata.Builder("copy")
            .addRequiredArg("index", Suggestions.Empty)
            .build()
    ) {

        val index = it[0].value.toLongOrNull() ?: run {
            sendEvent(Event.Message("Invalid index"))
            return@Leaf
        }

        val note = repository.getNote(index) ?: run {
            sendEvent(Event.Message("Invalid index"))
            return@Leaf
        }

        val manager = appContext.getSystemService<ClipboardManager>() ?: run {
            sendEvent(Event.Message("Clipboard isn't supported on your device"))
            return@Leaf
        }

        manager.setPrimaryClip(ClipData.newPlainText("note", note.content))
    }

    override val commands: CommandList = CommandList.Builder()
        .putCommand(ADD_OPTION)
        .putCommand(LIST_OPTION)
        .putCommand(REMOVE_OPTION)
        .putCommand(CLEAR_OPTION)
        .putCommand(COPY_OPTION)
        .build()
}

private val APP_COMMAND_GROUP = object : Command.Group("apps") {

    private val LIST_OPTION = Leaf(Metadata.Builder("ls").build()) {
        repository.loadLauncherApps().forEach {
            sendEvent(Event.Message(it.name) {
                appContext.startActivity(it.intent)
            })
        }
    }

    private val REMOVE_OPTION = Leaf(
        Metadata.Builder("rm")
            .addRequiredArg("name", Suggestions.Applications)
            .build()
    ) {
        val apps = repository.loadLauncherApps()
        when (apps.size) {
            0 -> sendEvent(Event.Message("App not found"))
            1 -> sendEvent(Event.StartIntent(apps.first().uninstallIntent))
            else -> {

                sendEvent(Event.Message("There are more than one app with this name. Which one did you meant?"))

                apps.forEachIndexed { index, appModel ->
                    sendEvent(Event.Message("$index: ${appModel.packageName}"))
                }

                val index = prompt("Enter a valid index").toIntOrNull() ?: run {
                    sendEvent(Event.Message("Invalid index"))
                    return@Leaf
                }

                if (index !in apps.indices) {
                    sendEvent(Event.Message("Invalid index"))
                    return@Leaf
                }

                sendEvent(Event.StartIntent(apps[index].uninstallIntent))
            }
        }
    }

    override val commands: CommandList = CommandList.Builder()
        .putCommand(REMOVE_OPTION)
        .putCommand(LIST_OPTION)
        .build()
}

private val LS_COMMAND = Command.Leaf(Metadata.Builder("ls").build()) {
    repository.loadFiles(workingDir).forEach {
        sendEvent(Event.Message(it))
    }
}

private val MAKE_DIR_COMMAND = Command.Leaf(
    Metadata.Builder("mkdir")
        .addRequiredArg("name", Suggestions.Empty)
        .build()
) { arguments ->
    val fileName = arguments[0].value
    val file = if (fileName.startsWith('/'))
        File(fileName) else File(workingDir, fileName)
    when {
        file.exists() -> sendEvent(Event.Message("$fileName already exists"))
        !file.mkdirs() -> sendEvent(Event.Message("failed to create folder"))
    }
}

private val ECHO_COMMAND = Command.Leaf(
    Metadata.Builder("echo")
        .addRequiredNArgs("args", Suggestions.Empty)
        .build()
) { arguments ->
    sendEvent(Event.Message(arguments.joinToString(" ") { it.value }))
}

private val CLEAR_COMMAND = Command.Leaf(Metadata.Builder("clear").build()) {
    sendEvent(Event.Clear)
}

private val PWD_COMMAND = Command.Leaf(Metadata.Builder("pwd").build()) {
    sendEvent(Event.Message(workingDir.canonicalPath))
}

private val CD_COMMAND = Command.Leaf(
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
}

private val EXIT_COMMAND = Command.Leaf(Metadata.Builder("exit").build()) {
    sendEvent(Event.Exit)
}

internal val COMMANDS = CommandList.Builder()
    .putCommand(ECHO_COMMAND)
    .putCommand(CLEAR_COMMAND)
    .putCommand(APP_COMMAND_GROUP)
    .putCommand(PWD_COMMAND)
    .putCommand(LS_COMMAND)
    .putCommand(MAKE_DIR_COMMAND)
    .putCommand(CD_COMMAND)
    .putCommand(EXIT_COMMAND)
    .putCommand(NOTE_COMMAND_GROUP)
    .build()
