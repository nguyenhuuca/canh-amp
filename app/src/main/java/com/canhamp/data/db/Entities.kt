package com.canhamp.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TrackSource { MEDIASTORE, SAF }

@Entity(
    tableName = "saf_folders",
    indices = [Index(value = ["treeUri"], unique = true)],
)
data class SafFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val treeUri: String,
    val displayName: String,
    val addedAtSec: Long,
)

@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = SafFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["safFolderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["folderPath"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["dateAddedSec"]),
        Index(value = ["safFolderId"]),
    ],
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    /** [TrackSource] name. */
    val source: String,
    val mediaStoreId: Long? = null,
    val safFolderId: Long? = null,
    val filePath: String? = null,
    val folderPath: String,
    val fileName: String,
    val title: String,
    val artist: String,
    val album: String,
    val format: String,
    val mimeType: String? = null,
    val durationMs: Long,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val scanGeneration: Long,
)

@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey val position: Int,
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val bitrateKbps: Int? = null,
    val trackId: Long? = null,
)

@Entity(
    tableName = "eq_presets",
    indices = [Index(value = ["name"], unique = true)],
)
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val gainsCsv: String,
    val preampDb: Int,
    val createdAtSec: Long,
)

data class GroupRow(val name: String, val count: Int)
