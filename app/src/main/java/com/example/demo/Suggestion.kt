package com.example.demo

class Suggestion(internal val label: String, replacement: String = "$label ") {
    internal val replacement: String = wrap(replacement)
}

private fun wrap(value: String) = value.trim().run {
    if (none { it.isWhitespace() }) value else '"' + this + '"'
}
