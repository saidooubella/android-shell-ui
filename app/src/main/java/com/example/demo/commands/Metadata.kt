package com.example.demo.commands

import com.example.demo.Suggestions

internal class Metadata private constructor(
    internal val name: String,
    internal val params: List<Parameter>,
    private val minArgsCount: Int,
    private val maxArgsCount: Int,
) {

    fun validateCount(actualCount: Int) = when {
        actualCount > maxArgsCount -> CountCheckResult.TooManyArgs
        actualCount < minArgsCount -> CountCheckResult.TooFewArgs
        else -> CountCheckResult.ExactArgs
    }

    internal class Builder(private val name: String) : RequiredArg {

        private val params = mutableListOf<Parameter>()
        private var minArgs = 0
        private var maxArgs = 0

        override fun addRequiredArg(name: String, suggestions: Suggestions): RequiredArg {
            params += Parameter(name, suggestions, required = true, variadic = false)
            maxArgs += 1
            minArgs += 1
            return this
        }

        override fun addRequiredNArgs(name: String, suggestions: Suggestions): MetadataBuilder {
            params += Parameter(name, suggestions, required = true, variadic = true)
            maxArgs = Int.MAX_VALUE
            minArgs += 1
            return this
        }

        override fun addOptionalArg(name: String, suggestions: Suggestions): OptionalArg {
            params += Parameter(name, suggestions, required = false, variadic = false)
            maxArgs += 1
            return this
        }

        override fun addOptionalNArgs(name: String, suggestions: Suggestions): MetadataBuilder {
            params += Parameter(name, suggestions, required = false, variadic = true)
            maxArgs = Int.MAX_VALUE
            return this
        }

        override fun build() = Metadata(name, params.toList(), minArgs, maxArgs)
    }
}
