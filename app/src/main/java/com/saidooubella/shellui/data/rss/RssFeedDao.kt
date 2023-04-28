package com.saidooubella.shellui.data.rss

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface RssFeedDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rssFeed: RssFeed): Long

    @Query("DELETE FROM rss_feeds WHERE name = :feedName")
    suspend fun remove(feedName: String): Int

    @Query("SELECT * FROM rss_feeds")
    suspend fun getAll(): List<RssFeed>

    @Query("SELECT * FROM rss_feeds WHERE name = :feedName")
    suspend fun get(feedName: String): RssFeed?
}