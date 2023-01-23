package com.example.demo.commands.impls

import com.example.demo.commands.Command
import com.example.demo.commands.Metadata
import com.example.demo.shell.Action

internal val CLEAR_COMMAND = Command.Leaf(Metadata.Builder("clear").build()) {
    sendAction(Action.Clear)
}
