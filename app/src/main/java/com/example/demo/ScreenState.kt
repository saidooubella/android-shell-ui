package com.example.demo

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

internal data class ScreenState(
    internal val exit: Boolean = false,
    internal val isDark: Boolean = false,
    internal val isIdle: Boolean = true,
    internal val suggestions: SuggestionsResult = SuggestionsResult.EMPTY,
    internal val logs: PersistentList<LogItem> = persistentListOf(),
    internal val fieldText: TextFieldValue = TextFieldValue("")
)
