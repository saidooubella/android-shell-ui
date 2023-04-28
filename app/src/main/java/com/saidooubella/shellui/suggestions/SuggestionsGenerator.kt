package com.saidooubella.shellui.suggestions

import com.saidooubella.shellui.commands.Arguments
import com.saidooubella.shellui.commands.Command
import com.saidooubella.shellui.commands.CountCheckResult
import com.saidooubella.shellui.commands.Parameter
import com.saidooubella.shellui.models.Argument
import com.saidooubella.shellui.shell.ShellContext
import com.saidooubella.shellui.utils.OpenableApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun ShellContext.loadSuggestions(
    args: Arguments,
    lineLength: Int,
): SuggestionsResult = withContext(Dispatchers.Default) {
    val context = this@loadSuggestions
    if (args.isEmpty()) {
        SuggestionsResult(loadFirstSuggestions(context, ""), MergeAction.Append)
    } else {
        val first = args.first()
        val parent = commands[first.value]
        if (first.end == lineLength) {
            SuggestionsResult(
                loadFirstSuggestions(context, first.value),
                MergeAction.Replace(first.start, first.end)
            )
        } else if (parent == null) {
            SuggestionsResult.EMPTY
        } else {
            proceedCommand(context, args.dropFirst(), parent, lineLength)
        }
    }
}

private suspend fun loadFirstSuggestions(context: ShellContext, hint: String) = buildList {

    if (hint.isNotBlank()) {
        addAll(context.repository.loadLauncherApps { it.contains(hint, true) }
            .map { OpenableApp(it.name, it.packageName) })
    }

    addAll(Suggestions.Commands.load(context, hint))
}

private suspend fun proceedCommand(
    shell: ShellContext,
    args: Arguments,
    command: Command,
    lineLength: Int,
): SuggestionsResult {

    var current: Command = command
    var arguments: Arguments = args

    while (true) {

        if (current is Command.Leaf) {
            return handleLeafCommand(shell, current, arguments, lineLength)
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
    shell: ShellContext,
    command: Command.Leaf,
    args: Arguments,
    lineLength: Int,
): SuggestionsResult {

    if (args.isEmpty()) {
        return when (command.metadata.params.isNotEmpty()) {
            true -> {
                val suggestions = command.metadata.params.first().suggestions
                SuggestionsResult(suggestions.load(shell, ""), MergeAction.Append)
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
            SuggestionsResult(argsInfo[0].suggestions.load(shell, hint), mergeAction)
        }
        else -> SuggestionsResult.EMPTY
    }
}

private fun Collection<String>.toSuggestions(query: String) =
    filter { it.indexOf(query, ignoreCase = true) != -1 }.map { Suggestion(it) }

private inline fun <T> Collection<T>.dropWhileIndexed(block: (Int, T) -> Boolean): List<T> {
    var index = 0
    return dropWhile { block(index++, it) }
}
