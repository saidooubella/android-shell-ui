package com.saidooubella.shellui.commands.impls

import android.Manifest
import android.os.Build
import android.os.Environment
import com.saidooubella.shellui.commands.Command
import com.saidooubella.shellui.commands.Metadata
import com.saidooubella.shellui.shell.Action
import com.saidooubella.shellui.suggestions.Suggestions
import com.saidooubella.shellui.utils.MANAGE_FILES_SETTINGS
import java.io.IOException

internal val TOUCH_COMMAND = Command.Leaf(
    Metadata.Builder("touch").addRequiredNArgs("files", Suggestions.Empty).build()
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

    arguments.forEach { argument ->

        val file = normalizePath(argument.value).takeIf { file -> !file.exists() } ?: run {
            sendAction(Action.Message("'${argument.value}' already exists"))
            return@forEach
        }

        try {
            if (!file.createNewFile()) {
                sendAction(Action.Message("Cannot create '${argument.value}'"))
            }
        } catch (e: IOException) {
            sendAction(Action.Message("${e.message}"))
        }
    }
}
