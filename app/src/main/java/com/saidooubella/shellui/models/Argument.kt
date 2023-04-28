package com.saidooubella.shellui.models

import com.saidooubella.shellui.commands.Arguments

internal data class Argument(
    internal val value: String,
    internal val start: Int,
    internal val end: Int
) {
    override fun toString(): String = value
}

private fun String.isSpaceAt(i: Int): Boolean {
    return i < length && this[i].isWhitespace()
}

internal fun String.parseArguments(): Arguments {

    val arguments = mutableListOf<Argument>()
    val builder = StringBuilder()
    var index = 0

    while (index < length) {

        while (isSpaceAt(index)) {
            index += 1
        }

        var escape = false
        var quote = false
        val start = index

        builder.delete(0, builder.length)

        while (index < length) {

            val c = this[index]

            if (escape) {
                escape = false
                builder.append(c)
                index++
                continue
            }

            if (!quote && c.isWhitespace()) {
                break
            }

            when (c) {
                '\\' -> escape = true
                '"' -> quote = !quote
                else -> builder.append(c)
            }

            index += 1
        }

        if (start >= index) {
            continue
        }

        arguments.add(Argument(builder.toString(), start, index))
    }

    return Arguments(arguments.toList())
}
