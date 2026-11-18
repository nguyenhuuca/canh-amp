package com.canhamp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TrackEntity::class, SafFolderEntity::class, QueueItemEntity::class, EqPresetEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CanhAmpDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun safFolderDao(): SafFolderDao
    abstract fun queueDao(): QueueDao
    abstract fun eqPresetDao(): EqPresetDao

    companion object {
        const val NAME = "canhamp.db"
    }
}
