package com.saidooubella.shellui.commands.impls

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import com.saidooubella.shellui.commands.Command
import com.saidooubella.shellui.commands.CommandList
import com.saidooubella.shellui.commands.Metadata
import com.saidooubella.shellui.shell.Action
import com.saidooubella.shellui.utils.hasNotPermission

internal val CONTACTS_COMMAND_GROUP = object : Command.Group("contacts") {

    private val LS_OPTION = Leaf(Metadata.Builder("ls").build()) {

        var permissions = arrayOf(Manifest.permission.READ_CONTACTS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissions += Manifest.permission.READ_PHONE_NUMBERS
        }

        if (permissions.any { appContext.hasNotPermission(it) }) {
            if (!sendAction(Action.RequestPermissions(permissions))) {
                sendAction(Action.Message("Can't read contacts"))
                return@Leaf
            }
        }

        repository.loadContacts().forEach { contact ->

            sendAction(Action.Message("${contact.name}: ${contact.phone}") {

                if (appContext.hasNotPermission(Manifest.permission.CALL_PHONE)) {
                    if (!sendAction(Action.RequestPermissions(arrayOf(Manifest.permission.CALL_PHONE)))) {
                        sendAction(Action.Message("Can't perform phone calls"))
                        return@Message
                    }
                }

                sendAction(Action.StartIntent(Intent(Intent.ACTION_CALL, "tel:${contact.phone}".toUri())))
            })
        }
    }

    override val commands: CommandList = CommandList.Builder()
        .putCommand(LS_OPTION)
        .build()
}
