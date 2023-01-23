package com.example.demo.commands.impls

import com.example.demo.commands.Command
import com.example.demo.commands.CommandList
import com.example.demo.commands.Metadata
import com.example.demo.data.rss.RssFeed
import com.example.demo.shell.Action
import com.example.demo.suggestions.Suggestions
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
