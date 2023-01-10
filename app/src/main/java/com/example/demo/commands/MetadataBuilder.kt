package com.example.demo.commands

import com.example.demo.Suggestions

internal interface MetadataBuilder {
    fun build(): Metadata
}

internal interface OptionalNArgs : MetadataBuilder {
    fun addOptionalNArgs(name: String, suggestions: Suggestions): MetadataBuilder
}

internal interface OptionalArg : OptionalNArgs {
    fun addOptionalArg(name: String, suggestions: Suggestions): OptionalArg
}

internal interface RequiredNArgs : OptionalArg {
    fun addRequiredNArgs(name: String, suggestions: Suggestions): MetadataBuilder
}

internal interface RequiredArg : RequiredNArgs {
    fun addRequiredArg(name: String, suggestions: Suggestions): RequiredArg
}
