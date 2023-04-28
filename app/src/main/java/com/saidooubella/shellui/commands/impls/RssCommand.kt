package com.saidooubella.shellui.commands.impls

import com.saidooubella.shellui.commands.Command
import com.saidooubella.shellui.commands.CommandList
import com.saidooubella.shellui.commands.Metadata
import com.saidooubella.shellui.data.rss.RssFeed
import com.saidooubella.shellui.shell.Action
import com.saidooubella.shellui.suggestions.Suggestions
import com.prof.rssparser.Parser

private val SPACE_REGEX = "[\t ]+".toRegex()
private val TAG_REGEX = "<.*?>".toRegex()

internal val RSS_COMMAND = object : Command.Group("rss") {

    private val ADD_OPTION = Leaf(
        Metadata.Builder("add")
            .addRequiredArg("name", Suggestions.Empty)
            .addRequiredArg("url", Suggestions.Empty)
            .build()
    ) {
        if (!repository.insertFeed(RssFeed(it[0].value, it[1].value))) {
            sendAction(Action.Message("Failed to add '${it[0].value}' feed"))
        }
    }

    private val LS_OPTION = Leaf(
        Metadata.Builder("ls")
            .addRequiredArg("name", Suggestions.Empty)
            .build()
    ) {

        val feed = repository.getFeed(it[0].value) ?: run {
            sendAction(Action.Message("'${it[0].value}' not found"))
            return@Leaf
        }

        val channel = Parser.Builder().build().getChannel(feed.url)

        sendAction(Action.Message(channel.title ?: "Untitled"))
        if (channel.description != null) {
            sendAction(Action.Message(channel.description!!))
        }

        channel.articles.take(5).forEach { article ->
            sendAction(Action.Message(article.title ?: "Untitled"))
            if (article.description != null) {
                val content = cleanFromHtmlTags(article.description!!)
                sendAction(Action.Message(content))
            }
        }
    }

    fun cleanFromHtmlTags(source: String): String {
        return source.replace(TAG_REGEX, "")
            .replace(SPACE_REGEX, " ").trim()
    }

    override val commands: CommandList = CommandList.Builder()
        .putCommand(ADD_OPTION)
        .putCommand(LS_OPTION)
        .build()
}
