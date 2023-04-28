package com.saidooubella.shellui.suggestions

internal data class SuggestionsResult(
    internal val suggestions: List<Suggestion>,
    internal val mergeAction: MergeAction,
) {
    companion object {
        internal val EMPTY = SuggestionsResult(emptyList(), MergeAction.Append)
    }
}
