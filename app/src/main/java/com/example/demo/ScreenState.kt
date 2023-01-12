package com.example.demo

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred

internal data class ScreenState(

    internal val isDark: Boolean = false,
    internal val isIdle: Boolean = true,
    internal val exit: Boolean = false,

    internal val intentForResult: IntentForResult? = null,
    internal val permissions: PermissionsHandler? = null,
    internal val intent: Intent? = null,

    internal val suggestions: SuggestionsResult = SuggestionsResult.EMPTY,
    internal val logs: PersistentList<LogItem> = persistentListOf(),
    internal val fieldText: TextFieldValue = TextFieldValue(""),
    internal val mode: ShellMode = ShellMode.RegularMode,
)

internal class IntentForResult(
    internal val intent: Intent,
    internal val continuation: CompletableDeferred<ActivityResult>,
    internal val triggered: Boolean = false,
) {
    fun triggered() = IntentForResult(intent, continuation, true)
}

internal class PermissionsHandler(
    internal val permissions: Array<String>,
    internal val continuation: CompletableDeferred<Boolean>,
    internal val triggered: Boolean = false,
) {
    fun triggered() = PermissionsHandler(permissions, continuation, true)
}

internal sealed interface ShellMode {
    object RegularMode : ShellMode
    data class PromptMode(val hint: String, val deferred: CompletableDeferred<String>) : ShellMode
}
