package com.example.demo

internal data class LogItem(
    internal val message: String,
    internal val action: (suspend () -> Unit)?,
)
