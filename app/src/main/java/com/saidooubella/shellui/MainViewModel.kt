package com.saidooubella.shellui

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.saidooubella.shellui.commands.*
import com.saidooubella.shellui.data.DataRepository
import com.saidooubella.shellui.models.parseArguments
import com.saidooubella.shellui.preferences.ShellPreferences
import com.saidooubella.shellui.shell.Action
import com.saidooubella.shellui.shell.ShellContext
import com.saidooubella.shellui.suggestions.*
import com.saidooubella.shellui.utils.OpenableApp
import com.saidooubella.shellui.utils.catch
import com.saidooubella.shellui.utils.loadFromPackage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal class MainViewModel(
    private val preferences: ShellPreferences,
    repository: DataRepository,
    context: Application
) : ViewModel() {

    private var suggestionsJob: Job? = null
    private var execJob: Job? = null

    private val state = MutableStateFlow(ScreenState())

    private val context = object : ShellContext(context, repository) {

        private val stateUpdate = state::update

        override suspend fun <R> sendAction(action: Action<R>): R {
            return action.execute(stateUpdate)
        }
    }

    val screenState = state.asStateFlow()
    val inDarkTheme = preferences.getDarkTheme(viewModelScope)

    val pinned = repository.getPinnedApps().map { list ->
        list.mapNotNull { app ->
            catch {
                val manager = context.packageManager
                val label = manager.loadFromPackage(app.packageName).loadLabel(manager).toString()
                OpenableApp(label, app.packageName)
            }
        }
    }

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
                        exec(content.parseArguments(), Commands)
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
            val arguments = state.value.fieldText.text.parseArguments()
            val suggestions = context.loadSuggestions(arguments, state.value.fieldText.text.length)
            state.update { it.copy(suggestions = suggestions) }
        }
    }

    internal fun toggleTheme() {
        viewModelScope.launch { preferences.toggleDarkTheme() }
    }

    private suspend fun exec(arguments: Arguments, commands: CommandList) {
        when (arguments.isEmpty()) {
            true -> context.sendAction(Action.Message("Too few arguments"))
            else -> when (val command = commands[arguments.first().value]) {
                is Command.Group -> exec(arguments.dropFirst(), command.commands)
                is Command.Leaf -> arguments.dropFirst().let { leafArgs ->
                    when (command.metadata.validateCount(leafArgs.count())) {
                        CountCheckResult.TooManyArgs -> context.sendAction(Action.Message("Too many arguments"))
                        CountCheckResult.TooFewArgs -> context.sendAction(Action.Message("Too few arguments"))
                        CountCheckResult.ExactArgs -> command.action(context, leafArgs)
                    }
                }
                null -> context.sendAction(Action.Message("command not found ${arguments.first().value}"))
            }
        }
    }

    override fun onCleared() {
        context.onCleared()
        suggestionsJob?.cancel()
        execJob?.cancel()
    }

    internal fun finishExiting() {
        state.update { it.copy(exit = false) }
    }

    internal fun markIntentForResultTriggered() {
        val intentForResult = state.value.intentForResult ?: return
        state.update { it.copy(intentForResult = intentForResult.triggered()) }
    }

    internal fun markPermissionsTriggered() {
        val permissions = state.value.permissions ?: return
        state.update { it.copy(permissions = permissions.triggered()) }
    }

    internal fun finishIntentForResult() {
        state.update { it.copy(intentForResult = null) }
    }

    internal fun finishPermissions() {
        state.update { it.copy(permissions = null) }
    }

    internal fun finishIntent() {
        state.update { it.copy(intent = null) }
    }

    internal class Factory(
        private val application: Application,
        private val preferences: ShellPreferences,
        private val repository: DataRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(preferences, repository, application) as T
        }
    }
}
