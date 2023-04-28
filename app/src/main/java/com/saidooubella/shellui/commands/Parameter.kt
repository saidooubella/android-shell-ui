package com.saidooubella.shellui.commands

import com.saidooubella.shellui.suggestions.Suggestions

internal data class Parameter(
    internal val name: String,
    internal val suggestions: Suggestions,
    internal val required: Boolean,
    internal val variadic: Boolean,
)
