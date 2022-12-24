package com.example.demo

import android.content.Intent
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred

internal data class ScreenState(
    internal val exit: Boolean = false,
    internal val isDark: Boolean = false,
    internal val isIdle: Boolean = true,
    internal val intent: Intent? = null,
    internal val mode: ShellMode = ShellMode.RegularMode,
    internal val suggestions: SuggestionsResult = SuggestionsResult.EMPTY,
    internal val logs: PersistentList<LogItem> = persistentListOf(),
    internal val fieldText: TextFieldValue = TextFieldValue("")
)

internal sealed interface ShellMode {
    object RegularMode : ShellMode
    data class PromptMode(val hint: String, val deferred: CompletableDeferred<String>) : ShellMode
}
