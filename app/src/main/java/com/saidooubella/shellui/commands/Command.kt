package com.saidooubella.shellui.commands

import com.saidooubella.shellui.shell.ShellContext

internal sealed class Command(val name: String) {

    internal data class Leaf(
        val metadata: Metadata,
        val action: suspend ShellContext.(Arguments) -> Unit,
    ) : Command(metadata.name)

    internal abstract class Group(name: String) : Command(name) {
        abstract val commands: CommandList
    }
}
