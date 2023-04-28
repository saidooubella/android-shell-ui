package com.saidooubella.shellui.commands.impls

import android.Manifest
import android.os.Build
import android.os.Environment
import com.saidooubella.shellui.commands.Command
import com.saidooubella.shellui.commands.Metadata
import com.saidooubella.shellui.shell.Action
import com.saidooubella.shellui.suggestions.Suggestions
import com.saidooubella.shellui.utils.MANAGE_FILES_SETTINGS

internal val MAKE_DIR_COMMAND = Command.Leaf(
    Metadata.Builder("mkdir").addRequiredArg("name", Suggestions.Empty).build()
) { arguments ->

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            sendAction(Action.StartIntentForResult(MANAGE_FILES_SETTINGS))
            if (!Environment.isExternalStorageManager()) {
                sendAction(Action.Message("Can't write to the external storage"))
                return@Leaf
            }
        }
    } else {
        if (!sendAction(Action.RequestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)))) {
            sendAction(Action.Message("Can't write to the external storage"))
            return@Leaf
        }
    }

    val fileName = arguments[0].value
    val file = normalizePath(fileName)

    when {
        file.exists() -> sendAction(Action.Message("$fileName already exists"))
        !file.mkdirs() -> sendAction(Action.Message("failed to create folder"))
    }
}
