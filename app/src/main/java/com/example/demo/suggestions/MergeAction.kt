package com.example.demo.suggestions

internal sealed interface MergeAction {
    class Replace(val start: Int, val end: Int) : MergeAction
    object Append : MergeAction
}