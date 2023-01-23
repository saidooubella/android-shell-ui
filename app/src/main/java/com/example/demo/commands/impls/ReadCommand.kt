package com.example.demo.commands.impls

import android.Manifest
import android.os.Build
import android.os.Environment
import com.example.demo.commands.Command
import com.example.demo.commands.Metadata
import com.example.demo.shell.Action
import com.example.demo.suggestions.Suggestions
import com.example.demo.utils.MANAGE_FILES_SETTINGS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileReader

internal val READ_COMMAND = Command.Leaf(
    Metadata.Builder("read").addRequiredArg("file", Suggestions.Files).build()
) { arguments ->

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

    val fileName = arguments[0].value

    val file = normalizePath(fileName).takeIf { it.exists() } ?: run {
        sendAction(Action.Message("$fileName is not found"))
        return@Leaf
    }

    withContext(Dispatchers.IO) {
        FileReader(file).buffered().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                sendAction(Action.Message(line))
            }
        }
    }
}
