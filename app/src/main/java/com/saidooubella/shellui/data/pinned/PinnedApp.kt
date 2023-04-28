package com.saidooubella.shellui.data.pinned

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_apps")
internal data class PinnedApp(
    @PrimaryKey(autoGenerate = true)
    internal val id: Long = 0L,
    @ColumnInfo(name = "package_name")
    internal val packageName: String = "",
)
