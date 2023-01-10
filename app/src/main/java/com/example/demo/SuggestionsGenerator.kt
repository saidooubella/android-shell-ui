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
            return@withContext SuggestionsResult(Suggestions.Commands.supply(shell, ""), MergeAction.Append)
        }
        val first: Argument = args[0]
        val parent: Command? = shell.commands[first.value]
        if (parent == null || first.end == lineLength) {
            return@withContext when (args.count() > 1 || first.end != lineLength) {
                true -> SuggestionsResult.EMPTY
                else -> SuggestionsResult(
                    filterSuggestions(shell.commands.names(), first.value),
                    MergeAction.Replace(first.start, first.end)
                )
            }
        }
        return@withContext proceedCommand(args, parent, lineLength)
    }

    private suspend fun proceedCommand(
        args: Arguments,
        command: Command,
        lineLength: Int,
    ): SuggestionsResult {
        var arguments: Arguments = args
        var current: Command? = command
        while (true) {
            arguments = arguments.dropFirst()
            if (current is Command.Group) {
                val set = current
                if (arguments.isEmpty()) {
                    return SuggestionsResult(filterSuggestions(set.commands.names(), ""), MergeAction.Append)
                }
                val optionArg: Argument = arguments[0]
                current = set.commands[optionArg.value]
                if (current == null || optionArg.end == lineLength) {
                    if (arguments.count() > 1 || optionArg.end != lineLength) {
                        return SuggestionsResult.EMPTY
                    }
                    return SuggestionsResult(filterSuggestions(set.commands.names(),
                        optionArg.value), MergeAction.Replace(optionArg.start, optionArg.end))
                }
            } else return if (current is Command.Leaf) {
                handleLeafCommand(current, arguments, lineLength)
            } else {
                throw IllegalStateException()
            }
        }
    }

    private suspend fun handleLeafCommand(
        command: Command.Leaf,
        args: Arguments,
        lineLength: Int,
    ): SuggestionsResult {
        if (args.isEmpty()) {
            if (command.metadata.params.isEmpty()) {
                return SuggestionsResult.EMPTY
            }
            return SuggestionsResult(command.metadata.params[0].suggestions.supply(shell, ""), MergeAction.Append)
        }
        if (command.metadata.validateCount(args.count()) == CountCheckResult.TooManyArgs) {
            return SuggestionsResult.EMPTY
        }
        val lastArg: Argument = args[args.count() - 1]
        val hint = if (lastArg.end == lineLength) lastArg.value else ""
        val target = if (lastArg.end == lineLength) args.count() - 1 else args.count()
        var argsInfo: List<Parameter> = command.metadata.params
        var i = 0
        while (i < target && argsInfo.isNotEmpty()) {
            if (!argsInfo[0].variadic) {
                argsInfo = argsInfo.subList(1, argsInfo.size)
            }
            i++
        }
        val mergeAction: MergeAction = if (lastArg.end == lineLength)
            MergeAction.Replace(lastArg.start, lastArg.end) else MergeAction.Append
        if (argsInfo.isEmpty()) {
            return SuggestionsResult.EMPTY
        }
        return SuggestionsResult(argsInfo[0].suggestions.supply(shell, hint), mergeAction)
    }

    private fun filterSuggestions(names: Collection<String>, query: String): List<Suggestion> {
        return names.filter { it.indexOf(query) != -1 }.map { Suggestion(it) }
    }
}
