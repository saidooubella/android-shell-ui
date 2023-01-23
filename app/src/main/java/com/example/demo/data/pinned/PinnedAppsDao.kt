package com.example.demo.data.pinned

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PinnedAppsDao {

    @Query("SELECT * FROM pinned_apps")
    fun get(): Flow<List<PinnedApp>>

    @Query("SELECT * FROM pinned_apps")
    suspend fun getList(): List<PinnedApp>

    @Insert
    suspend fun insert(pinnedApp: PinnedApp)

    @Query("DELETE FROM pinned_apps WHERE package_name = :packageName")
    suspend fun remove(packageName: String): Int
}
