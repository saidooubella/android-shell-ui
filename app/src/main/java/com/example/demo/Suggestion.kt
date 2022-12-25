package com.example.demo

class Suggestion private constructor(
    internal val label: String,
    internal val replacement: String,
) {
    companion object {

        fun of(label: String, value: String = label) = Suggestion(label, wrap(value) + ' ')

        private fun wrap(value: String) = if (value.trim().none { it.isWhitespace() }) value else '"' + value.trim() + '"'
    }
}

val String.asSuggestion: Suggestion
    get() = Suggestion.of(this, this)
