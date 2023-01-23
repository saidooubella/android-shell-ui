package com.example.demo.commands.impls

import android.Manifest
import android.os.Build
import android.os.Environment
import com.example.demo.commands.Command
import com.example.demo.commands.Metadata
import com.example.demo.shell.Action
import com.example.demo.suggestions.Suggestions
import com.example.demo.utils.MANAGE_FILES_SETTINGS

internal val RM_COMMAND = Command.Leaf(
    Metadata.Builder("rm").addRequiredNArgs("files", Suggestions.Files).build()
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

    arguments.forEach {
        if (!normalizePath(it.value).deleteRecursively()) {
            sendAction(Action.Message("Can't delete ${it.value}"))
        }
    }
}
