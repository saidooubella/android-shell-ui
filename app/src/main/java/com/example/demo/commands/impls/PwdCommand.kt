package com.example.demo.commands.impls

import com.example.demo.commands.Command
import com.example.demo.commands.Metadata
import com.example.demo.shell.Action

internal val PWD_COMMAND = Command.Leaf(Metadata.Builder("pwd").build()) {
    sendAction(Action.Message(workingDir.canonicalPath))
}
