package com.example.demo

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred

internal data class ScreenState(
    internal val exit: Boolean = false,
    internal val isDark: Boolean = false,
    internal val isIdle: Boolean = true,
    internal val intent: Intent? = null,
    internal val intentForResult: IntentForResult? = null,
    internal val permissions: PermissionsHandler? = null,
    internal val mode: ShellMode = ShellMode.RegularMode,
    internal val suggestions: SuggestionsResult = SuggestionsResult.EMPTY,
    internal val logs: PersistentList<LogItem> = persistentListOf(),
    internal val fieldText: TextFieldValue = TextFieldValue("")
)

internal class IntentForResult(
    internal val intent: Intent,
    internal val continuation: CompletableDeferred<ActivityResult>,
)

internal class PermissionsHandler(
    internal val permissions: Array<String>,
    internal val continuation: CompletableDeferred<Boolean>,
)

internal sealed interface ShellMode {
    object RegularMode : ShellMode
    data class PromptMode(val hint: String, val deferred: CompletableDeferred<String>) : ShellMode
}
