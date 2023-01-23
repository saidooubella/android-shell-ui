package com.example.demo.commands.impls

import com.example.demo.commands.Command
import com.example.demo.commands.Metadata
import com.example.demo.shell.Action
import com.example.demo.suggestions.Suggestions

internal val ECHO_COMMAND = Command.Leaf(
    Metadata.Builder("echo")
        .addRequiredNArgs("args", Suggestions.Empty)
        .build()
) { arguments ->
    sendAction(Action.Message(arguments.joinToString(" ") { it.value }))
}
