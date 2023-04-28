package com.saidooubella.shellui.commands.impls

import android.content.ClipData
import android.content.ClipboardManager
import androidx.core.content.getSystemService
import com.saidooubella.shellui.commands.Command
import com.saidooubella.shellui.commands.CommandList
import com.saidooubella.shellui.commands.Metadata
import com.saidooubella.shellui.data.notes.Note
import com.saidooubella.shellui.shell.Action
import com.saidooubella.shellui.suggestions.Suggestions

internal val NOTE_COMMAND_GROUP = object : Command.Group("notes") {

    private val ADD_OPTION = Leaf(
        Metadata.Builder("add")
            .addRequiredArg("note", Suggestions.Empty)
            .build()
    ) {
        repository.addNote(Note(content = it[0].value))
    }

    private val LIST_OPTION = Leaf(Metadata.Builder("list").build()) {
        repository.notesList().forEachIndexed { index, note ->
            sendAction(Action.Message("$index: ${note.content}"))
        }
    }

    private val REMOVE_OPTION = Leaf(
        Metadata.Builder("rm")
            .addRequiredArg("index", Suggestions.Empty)
            .build()
    ) { arguments ->
        arguments[0].value.toLongOrNull()?.takeIf { repository.removeNote(it) > 0 } ?: run {
            sendAction(Action.Message("Invalid index"))
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
            sendAction(Action.Message("Invalid index"))
            return@Leaf
        }

        val note = repository.getNote(index) ?: run {
            sendAction(Action.Message("Invalid index"))
            return@Leaf
        }

        val manager = appContext.getSystemService<ClipboardManager>() ?: run {
            sendAction(Action.Message("Clipboard isn't supported on your device"))
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
