package com.example.demo.data.rss

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rss_feeds")
internal data class RssFeed(
    @PrimaryKey
    internal val name: String,
    internal val url: String,
)
