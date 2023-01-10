package com.example.demo

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.demo.commands.Arguments
import com.example.demo.commands.Command
import com.example.demo.commands.CommandList
import com.example.demo.commands.CountCheckResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal class ScreenViewModel(repository: Repository, context: Application) : ViewModel() {

    private var suggestionsJob: Job? = null
    private var execJob: Job? = null

    private val state = MutableStateFlow(ScreenState())

    private val context = object : ShellContext(context, repository, COMMANDS) {
        override suspend fun <R> sendAction(action: Action<R>) = action.execute(state)
    }

    val screenState = state.asStateFlow()

    init {
        viewModelScope.launch {
            state.distinctUntilChanged { old, new ->
                old.fieldText.text == new.fieldText.text && old.mode == new.mode
            }.collectLatest { generateSuggestions() }
        }
    }

    internal fun submitLine() {

        val content = state.value.fieldText.text.trim()
        state.update { it.copy(fieldText = TextFieldValue("")) }

        if (content.isBlank()) {
            return
        }

        when (val shellMode = state.value.mode) {
            is ShellMode.PromptMode -> {
                shellMode.deferred.complete(content)
                state.update { it.copy(mode = ShellMode.RegularMode) }
            }
            is ShellMode.RegularMode -> {
                execJob?.cancel()
                execJob = viewModelScope.launch {
                    state.update { it.copy(isIdle = false) }
                    try {
                        context.sendAction(Action.Message(">> $content"))
                        exec(content.toArguments(), COMMANDS)
                    } finally {
                        state.update { it.copy(isIdle = true) }
                    }
                }
            }
        }
    }

    internal fun changeFieldText(content: TextFieldValue) {
        state.update {
            it.copy(fieldText = content.copy(content.text.replace('\n', ' ')))
        }
    }

    private fun generateSuggestions() {

        suggestionsJob?.cancel()
        suggestionsJob = null

        if (state.value.mode !is ShellMode.RegularMode) {
            state.update { it.copy(suggestions = SuggestionsResult.EMPTY) }
            return
        }

        suggestionsJob = viewModelScope.launch {
            val arguments = state.value.fieldText.text.toArguments()
            val suggestions = SuggestionsGenerator(context)
                .suggestions(arguments, state.value.fieldText.text.length)
            state.update { it.copy(suggestions = suggestions) }
        }
    }

    internal fun toggleTheme() {
        state.update { it.copy(isDark = !it.isDark) }
    }

    private suspend fun exec(arguments: Arguments, commands: CommandList) {
        when (arguments.isEmpty()) {
            true -> context.sendAction(Action.Message("Too few arguments"))
            else -> when (val command = commands[arguments[0].value]) {
                is Command.Group -> exec(arguments.dropFirst(), command.commands)
                is Command.Leaf -> arguments.dropFirst().let { leafArgs ->
                    when (command.metadata.validateCount(leafArgs.count())) {
                        CountCheckResult.TooManyArgs -> context.sendAction(Action.Message("Too many arguments"))
                        CountCheckResult.TooFewArgs -> context.sendAction(Action.Message("Too few arguments"))
                        CountCheckResult.ExactArgs -> command.action(context, leafArgs)
                    }
                }
                null -> context.sendAction(Action.Message("command not found ${arguments[0].value}"))
            }
        }
    }

    override fun onCleared() {
        suggestionsJob?.cancel()
        execJob?.cancel()
    }

    internal fun finishExiting() {
        state.update { it.copy(exit = false) }
    }

    internal fun finishIntentForResult() {
        state.update { it.copy(intentForResult = null) }
    }

    internal fun finishIntent() {
        state.update { it.copy(intent = null) }
    }

    internal fun finishPermissions() {
        state.update { it.copy(permissions = null) }
    }

    internal class Factory(
        private val application: Application,
        private val repository: Repository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScreenViewModel(repository, application) as T
        }
    }
}

data class Argument(val value: String, val start: Int, val end: Int) {
    override fun toString(): String = value
}

private fun String.isSpaceAt(i: Int): Boolean {
    return i < length && this[i].isWhitespace()
}

private fun String.toArguments(): Arguments {

    val arguments = mutableListOf<Argument>()
    val builder = StringBuilder()
    var index = 0

    while (index < length) {

        while (isSpaceAt(index)) {
            index += 1
        }

        var escape = false
        var quote = false
        val start = index

        builder.delete(0, builder.length)

        while (index < length) {

            val c = this[index]

            if (escape) {
                escape = false
                builder.append(c)
                index++
                continue
            }

            if (!quote && c.isWhitespace()) {
                break
            }

            when (c) {
                '\\' -> escape = true
                '"' -> quote = !quote
                else -> builder.append(c)
            }

            index += 1
        }

        if (start >= index) {
            continue
        }

        arguments.add(Argument(builder.toString(), start, index))
    }

    return Arguments(arguments.toList())
}
