package com.example.demo.commands

import com.example.demo.ShellContext

internal sealed class Command(val name: String) {

    internal data class Leaf(
        val metadata: Metadata,
        val action: suspend ShellContext.(Arguments) -> Unit,
    ) : Command(metadata.name)

    internal abstract class Group(name: String) : Command(name) {
        abstract val commands: CommandList
    }
}
