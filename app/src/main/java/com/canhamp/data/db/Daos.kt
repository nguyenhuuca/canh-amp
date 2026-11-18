package com.canhamp.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val SEARCH =
    "(:pattern = '' OR title LIKE :pattern ESCAPE '\\' OR artist LIKE :pattern ESCAPE '\\' " +
        "OR album LIKE :pattern ESCAPE '\\' OR fileName LIKE :pattern ESCAPE '\\')"

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(tracks: List<TrackEntity>): List<Long>

    @Update
    suspend fun update(tracks: List<TrackEntity>)

    @Query("SELECT id FROM tracks WHERE uri = :uri")
    suspend fun idForUri(uri: String): Long?

    /** Inserts new rows; rows whose uri already exists are updated in place and keep their id. Returns how many rows were newly inserted. */
    @Transaction
    suspend fun upsertByUri(tracks: List<TrackEntity>): Int {
        val results = insertIgnore(tracks)
        val toUpdate = ArrayList<TrackEntity>()
        for ((i, rowId) in results.withIndex()) {
            if (rowId == -1L) {
                val id = idForUri(tracks[i].uri) ?: continue
                toUpdate.add(tracks[i].copy(id = id))
            }
        }
        if (toUpdate.isNotEmpty()) update(toUpdate)
        return tracks.size - toUpdate.size
    }

    @Query("DELETE FROM tracks WHERE source = :source AND scanGeneration < :generation")
    suspend fun deleteStale(source: String, generation: Long): Int

    @Query("DELETE FROM tracks WHERE safFolderId = :id")
    suspend fun deleteBySafFolder(id: Long): Int

    @Query("SELECT COUNT(*) FROM tracks")
    fun count(): Flow<Int>

    @Query("SELECT * FROM tracks WHERE uri = :uri")
    suspend fun findByUri(uri: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE filePath = :path LIMIT 1")
    suspend fun findByFilePath(path: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun findById(id: Long): TrackEntity?

    @Query("UPDATE tracks SET sampleRateHz = :hz WHERE id = :id")
    suspend fun updateSampleRate(id: Long, hz: Int)

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM tracks")
    suspend fun sumSize(): Long

    @Query("SELECT * FROM tracks WHERE $SEARCH ORDER BY title COLLATE NOCASE ASC, id ASC")
    fun pagingAll(pattern: String): PagingSource<Int, TrackEntity>

    @Query("SELECT * FROM tracks WHERE folderPath = :folder AND $SEARCH ORDER BY title COLLATE NOCASE ASC, id ASC")
    fun pagingByFolder(folder: String, pattern: String): PagingSource<Int, TrackEntity>

    @Query("SELECT * FROM tracks WHERE artist = :artist AND $SEARCH ORDER BY title COLLATE NOCASE ASC, id ASC")
    fun pagingByArtist(artist: String, pattern: String): PagingSource<Int, TrackEntity>

    @Query("SELECT * FROM tracks WHERE album = :album AND $SEARCH ORDER BY title COLLATE NOCASE ASC, id ASC")
    fun pagingByAlbum(album: String, pattern: String): PagingSource<Int, TrackEntity>

    @Query("SELECT * FROM tracks ORDER BY dateAddedSec DESC, id DESC LIMIT :limit")
    fun recent(limit: Int = 100): Flow<List<TrackEntity>>

    @Query("SELECT folderPath AS name, COUNT(*) AS count FROM tracks GROUP BY folderPath ORDER BY folderPath COLLATE NOCASE")
    fun folderGroups(): Flow<List<GroupRow>>

    @Query("SELECT artist AS name, COUNT(*) AS count FROM tracks GROUP BY artist ORDER BY artist COLLATE NOCASE")
    fun artistGroups(): Flow<List<GroupRow>>

    @Query("SELECT album AS name, COUNT(*) AS count FROM tracks GROUP BY album ORDER BY album COLLATE NOCASE")
    fun albumGroups(): Flow<List<GroupRow>>
}

@Dao
interface SafFolderDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(folder: SafFolderEntity): Long

    @Query("SELECT * FROM saf_folders ORDER BY addedAtSec")
    fun all(): Flow<List<SafFolderEntity>>

    @Query("SELECT * FROM saf_folders WHERE treeUri = :treeUri")
    suspend fun findByTreeUri(treeUri: String): SafFolderEntity?

    @Query("DELETE FROM saf_folders WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue_items ORDER BY position")
    suspend fun getAll(): List<QueueItemEntity>

    @Query("SELECT * FROM queue_items ORDER BY position")
    fun observe(): Flow<List<QueueItemEntity>>

    @Query("DELETE FROM queue_items")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QueueItemEntity>)

    @Transaction
    suspend fun replaceAll(items: List<QueueItemEntity>) {
        deleteAll()
        insertAll(items)
    }
}

@Dao
interface EqPresetDao {
    @Query("SELECT * FROM eq_presets ORDER BY createdAtSec, id")
    fun observe(): Flow<List<EqPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: EqPresetEntity): Long

    @Query("SELECT * FROM eq_presets WHERE name = :name")
    suspend fun findByName(name: String): EqPresetEntity?

    @Query("DELETE FROM eq_presets WHERE id = :id")
    suspend fun delete(id: Long)
}
