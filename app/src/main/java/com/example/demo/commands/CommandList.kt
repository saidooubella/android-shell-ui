package com.example.demo.commands

import java.util.*

internal class CommandList private constructor(
    private val commands: Map<String, Command>,
): Iterable<Command> {

    internal operator fun get(name: String) = commands[name]

    internal fun names() = commands.keys

    override fun iterator() = commands.values.iterator()

    internal class Builder {

        private val commands = TreeMap<String, Command>()

        internal fun putCommand(command: Command): Builder {
            commands[command.name] = command
            return this
        }

        internal fun build() = CommandList(commands.toMap())
    }
}