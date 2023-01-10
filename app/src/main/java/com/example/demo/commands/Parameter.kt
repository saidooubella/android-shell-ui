package com.example.demo.commands

import com.example.demo.Suggestions

internal data class Parameter(
    internal val name: String,
    internal val suggestions: Suggestions,
    internal val required: Boolean,
    internal val variadic: Boolean,
)
