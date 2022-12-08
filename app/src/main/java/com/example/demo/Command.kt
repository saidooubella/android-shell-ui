package com.example.demo

internal sealed interface Command {

    val name: String

    data class Leaf(
        val metadata: Metadata,
        val action: suspend ShellContext.(Arguments) -> Unit,
    ) : Command {
        override val name: String = metadata.name
    }

    data class Group(override val name: String, val commands: CommandList) : Command
}

internal sealed interface CountValidationResult
internal object TooManyArgs : CountValidationResult
internal object TooFewArgs : CountValidationResult
internal object ExactArgs : CountValidationResult

internal class Metadata private constructor(
    internal val name: String,
    internal val params: List<Parameter>,
    private val minArgsCount: Int,
    private val maxArgsCount: Int,
) {

    fun validateCount(actualCount: Int) = when {
        actualCount > maxArgsCount -> TooManyArgs
        actualCount < minArgsCount -> TooFewArgs
        else -> ExactArgs
    }

    internal class Builder(private val name: String) : RequiredArg {

        private val params = mutableListOf<Parameter>()
        private var minArgs = 0
        private var maxArgs = 0

        override fun addRequiredArg(name: String, suggestions: Suggestions): RequiredArg {
            params += Parameter(name, suggestions, required = true, variadic = false)
            maxArgs += 1
            minArgs += 1
            return this
        }

        override fun addRequiredNArgs(name: String, suggestions: Suggestions): MetadataBuilder {
            params += Parameter(name, suggestions, required = true, variadic = true)
            maxArgs = Int.MAX_VALUE
            minArgs += 1
            return this
        }

        override fun addOptionalArg(name: String, suggestions: Suggestions): OptionalArg {
            params += Parameter(name, suggestions, required = false, variadic = false)
            maxArgs += 1
            return this
        }

        override fun addOptionalNArgs(name: String, suggestions: Suggestions): MetadataBuilder {
            params += Parameter(name, suggestions, required = false, variadic = true)
            maxArgs = Int.MAX_VALUE
            return this
        }

        override fun build() = Metadata(name, params.toList(), minArgs, maxArgs)
    }
}

internal data class Parameter(
    internal val name: String,
    internal val suggestions: Suggestions,
    internal val required: Boolean,
    internal val variadic: Boolean,
)

internal interface MetadataBuilder {
    fun build(): Metadata
}

internal interface OptionalNArgs : MetadataBuilder {
    fun addOptionalNArgs(name: String, suggestions: Suggestions): MetadataBuilder
}

internal interface OptionalArg : OptionalNArgs {
    fun addOptionalArg(name: String, suggestions: Suggestions): OptionalArg
}

internal interface RequiredNArgs : OptionalArg {
    fun addRequiredNArgs(name: String, suggestions: Suggestions): MetadataBuilder
}

internal interface RequiredArg : RequiredNArgs {
    fun addRequiredArg(name: String, suggestions: Suggestions): RequiredArg
}

internal class Arguments(
    private val arguments: List<Argument>,
): Iterable<Argument> {
    internal operator fun get(index: Int) = arguments[index]
    internal fun count() = arguments.size
    internal fun dropFirst() = Arguments(arguments.drop(1))
    internal fun isEmpty() = arguments.isEmpty()
    override fun iterator() = arguments.iterator()
}

internal class CommandList private constructor(
    private val commands: Map<String, Command>,
): Iterable<Command> {

    internal operator fun get(name: String) = commands[name]

    internal fun names() = commands.keys

    override fun iterator() = commands.values.iterator()

    internal class Builder {

        private val commands = mutableMapOf<String, Command>()

        internal fun putCommand(command: Command): Builder {
            commands[command.name] = command
            return this
        }

        internal fun build() = CommandList(commands.toMap())
    }
}
