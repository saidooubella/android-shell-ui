package com.saidooubella.shellui.suggestions

class Suggestion(
    internal val label: String,
    replacement: String = "$label ",
    internal val runnable: Boolean = false,
) {
    internal val replacement: String = if (runnable) replacement else wrap(replacement)
}

private fun wrap(value: String) = value.trim().run {
    if (none { it.isWhitespace() }) value else '"' + this + '"'
}
