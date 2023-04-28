package com.saidooubella.shellui.models

internal data class LogItem(
    internal val message: String,
    internal val action: (suspend () -> Unit)?,
)
