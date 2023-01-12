package com.example.demo.suggestions

internal class SuggestionsResult(
    internal val suggestions: List<Suggestion>,
    internal val mergeAction: MergeAction,
) {
    companion object {
        internal val EMPTY = SuggestionsResult(emptyList(), MergeAction.Append)
    }
}
