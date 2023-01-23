package com.example.demo.commands.impls

import android.Manifest
import android.os.Build
import android.os.Environment
import com.example.demo.commands.Command
import com.example.demo.commands.Metadata
import com.example.demo.shell.Action
import com.example.demo.utils.MANAGE_FILES_SETTINGS

internal val LS_COMMAND = Command.Leaf(Metadata.Builder("ls").build()) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            sendAction(Action.StartIntentForResult(MANAGE_FILES_SETTINGS))
            if (!Environment.isExternalStorageManager()) {
                sendAction(Action.Message("Can't read from the external storage"))
                return@Leaf
            }
        }
    } else {
        if (!sendAction(Action.RequestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)))) {
            sendAction(Action.Message("Can't read from the external storage"))
            return@Leaf
        }
    }

    repository.loadFiles(workingDir).forEach {
        sendAction(Action.Message(it.name))
    }
}
