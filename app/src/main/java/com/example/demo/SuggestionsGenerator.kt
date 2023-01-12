package com.example.demo

import com.example.demo.commands.Arguments
import com.example.demo.commands.Command
import com.example.demo.commands.CountCheckResult
import com.example.demo.commands.Parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class SuggestionsResult(
    internal val suggestions: List<Suggestion>,
    internal val mergeAction: MergeAction,
) {
    companion object {
        internal val EMPTY = SuggestionsResult(emptyList(), MergeAction.Append)
    }
}

internal sealed interface MergeAction {
    class Replace(val start: Int, val end: Int) : MergeAction
    object Append : MergeAction
}

internal class SuggestionsGenerator(private val shell: ShellContext) {

    internal suspend fun suggestions(
        args: Arguments,
        lineLength: Int,
    ): SuggestionsResult = withContext(Dispatchers.Default) {
        if (args.isEmpty()) {
            SuggestionsResult(Suggestions.Commands.supply(shell, ""), MergeAction.Append)
        } else {
            val first = args.first()
            val parent = shell.commands[first.value]
            if (first.end == lineLength) {
                SuggestionsResult(
                    shell.commands.names().toSuggestions(first.value),
                    MergeAction.Replace(first.start, first.end)
                )
            } else if (parent == null) {
                SuggestionsResult.EMPTY
            } else {
                proceedCommand(args.dropFirst(), parent, lineLength)
            }
        }
    }

    private suspend fun proceedCommand(
        args: Arguments,
        command: Command,
        lineLength: Int,
    ): SuggestionsResult {

        var current: Command = command
        var arguments: Arguments = args

        while (true) {

            if (current is Command.Leaf) {
                return handleLeafCommand(current, arguments, lineLength)
            }

            if (current is Command.Group) {

                val option = if (arguments.isEmpty()) {
                    return SuggestionsResult(
                        current.commands.names().toSuggestions(""),
                        MergeAction.Append
                    )
                } else {
                    arguments.first()
                }

                val next = current.commands[option.value]

                if (option.end == lineLength) {
                    return SuggestionsResult(
                        current.commands.names().toSuggestions(option.value),
                        MergeAction.Replace(option.start, option.end)
                    )
                } else if (next == null) {
                    return SuggestionsResult.EMPTY
                }

                arguments = arguments.dropFirst()
                current = next
            }
        }
    }

    private suspend fun handleLeafCommand(
        command: Command.Leaf,
        args: Arguments,
        lineLength: Int,
    ): SuggestionsResult {

        if (args.isEmpty()) {
            return when (command.metadata.params.isNotEmpty()) {
                true -> {
                    val suggestions = command.metadata.params.first().suggestions
                    SuggestionsResult(suggestions.supply(shell, ""), MergeAction.Append)
                }
                else -> SuggestionsResult.EMPTY
            }
        }

        if (command.metadata.validateCount(args.count()) == CountCheckResult.TooManyArgs) {
            return SuggestionsResult.EMPTY
        }

        val last: Argument = args.last()
        val (hint, limit) = when (last.end == lineLength) {
            true -> last.value to args.count() - 1
            else -> "" to args.count()
        }

        val argsInfo: List<Parameter> = command.metadata.params
            .dropWhileIndexed { index, param -> index < limit && !param.variadic }

        return when (argsInfo.isNotEmpty()) {
            true -> {
                val mergeAction: MergeAction = when (last.end == lineLength) {
                    true -> MergeAction.Replace(last.start, last.end)
                    else -> MergeAction.Append
                }
                SuggestionsResult(argsInfo[0].suggestions.supply(shell, hint), mergeAction)
            }
            else -> SuggestionsResult.EMPTY
        }
    }
}

private fun Collection<String>.toSuggestions(query: String): List<Suggestion> =
    filter { it.indexOf(query) != -1 }.map { Suggestion(it) }

private inline fun <T> Collection<T>.dropWhileIndexed(block: (Int, T) -> Boolean): List<T> {
    var index = 0
    return dropWhile { block(index++, it) }
}
