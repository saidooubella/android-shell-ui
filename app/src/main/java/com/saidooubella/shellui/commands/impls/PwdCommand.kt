package com.saidooubella.shellui.commands.impls

import com.saidooubella.shellui.commands.Command
import com.saidooubella.shellui.commands.Metadata
import com.saidooubella.shellui.shell.Action

internal val PWD_COMMAND = Command.Leaf(Metadata.Builder("pwd").build()) {
    sendAction(Action.Message(workingDir.canonicalPath))
}
