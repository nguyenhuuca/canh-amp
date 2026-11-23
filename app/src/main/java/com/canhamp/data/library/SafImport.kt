package com.canhamp.data.library

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import com.canhamp.data.db.SafFolderDao
import com.canhamp.data.db.SafFolderEntity
import com.canhamp.data.db.TrackDao
import com.canhamp.data.db.TrackEntity
import com.canhamp.data.db.TrackSource

/** Persisted read grants on document trees. */
interface UriPermissions {
    /** Takes a persistable read grant; throws [SecurityException] when the system refuses. */
    fun take(treeUri: String)
    fun release(treeUri: String)
    fun persisted(): Set<String>
}

class ContentResolverUriPermissions(private val resolver: ContentResolver) : UriPermissions {
    override fun take(treeUri: String) =
        resolver.takePersistableUriPermission(Uri.parse(treeUri), Intent.FLAG_GRANT_READ_URI_PERMISSION)

    override fun release(treeUri: String) {
        try {
            resolver.releasePersistableUriPermission(Uri.parse(treeUri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // the grant was already gone
        }
    }

    override fun persisted(): Set<String> =
        resolver.persistedUriPermissions.filter { it.isReadPermission }.map { it.uri.toString() }.toSet()
}

/** The slice of the database an import needs. */
interface SafStore {
    suspend fun findFolder(treeUri: String): SafFolderEntity?

    /** Inserts the folder row; returns its id. */
    suspend fun insertFolder(folder: SafFolderEntity): Long
    suspend fun folders(): List<SafFolderEntity>
    suspend fun tracksOf(folderId: Long): List<TrackEntity>

    /** Updates existing rows only; a row deleted meanwhile is left deleted. */
    suspend fun updateTracks(tracks: List<TrackEntity>)

    /** Deletes the folder's tracks and then the folder row. */
    suspend fun removeFolder(folderId: Long)
}

class DaoSafStore(private val folders: SafFolderDao, private val tracks: TrackDao) : SafStore {
    override suspend fun findFolder(treeUri: String) = folders.findByTreeUri(treeUri)
    override suspend fun insertFolder(folder: SafFolderEntity) = folders.insert(folder)
    override suspend fun folders() = folders.allOnce()
    override suspend fun tracksOf(folderId: Long) = tracks.tracksOfSafFolder(folderId)
    override suspend fun updateTracks(tracks: List<TrackEntity>) = this.tracks.updateKeepingLearned(tracks)
    override suspend fun removeFolder(folderId: Long) {
        tracks.deleteBySafFolder(folderId)
        folders.delete(folderId)
    }
}

/** Everything [LibraryRepository.importFolder] needs besides the shared pieces. */
class SafImportDeps(
    val store: SafStore,
    val trees: DocumentTrees,
    val grants: UriPermissions,
)

data class ImportResult(val trackCount: Int)

sealed interface ImportOutcome {
    data class Success(val result: ImportResult) : ImportOutcome
    data object AlreadyImported : ImportOutcome
    data object Empty : ImportOutcome
    data object NotPersistable : ImportOutcome
    data object Failed : ImportOutcome
}

/** Pure mapping from a walked file to a first-pass [TrackEntity], with metadata from the file name (B10). */
object SafTrackMapper {
    const val UNKNOWN = TrackRowMapper.UNKNOWN
    const val ROOT = "/SAF"

    fun folderPath(displayName: String, relativeDir: String): String {
        val base = "$ROOT/${safeName(displayName)}"
        return if (relativeDir.isEmpty()) base else "$base/$relativeDir"
    }

    fun safeName(displayName: String): String =
        displayName.replace('/', '_').trim().ifEmpty { "folder" }.take(TrackRowMapper.MAX_TEXT)

    fun map(file: SafFile, folderId: Long, displayName: String, nowSec: Long): TrackEntity {
        val base = file.name.substringBeforeLast('.', file.name)
        val ext = file.name.substringAfterLast('.', "").uppercase()
        return TrackEntity(
            uri = file.uri,
            source = TrackSource.SAF.name,
            safFolderId = folderId,
            folderPath = folderPath(displayName, file.relativeDir),
            fileName = file.name,
            title = base.trim().ifEmpty { UNKNOWN }.take(TrackRowMapper.MAX_TEXT),
            artist = UNKNOWN,
            album = UNKNOWN,
            format = ext.ifEmpty { "AUDIO" },
            mimeType = file.mime,
            durationMs = 0L,
            sizeBytes = file.size,
            dateAddedSec = nowSec,
            dateModifiedSec = file.lastModified / 1000,
            scanGeneration = 0L,
        )
    }

    /** Overlays file tags on a name-derived row; blank or missing tags keep the existing value. */
    fun enrich(row: TrackEntity, tags: TrackTags): TrackEntity = row.copy(
        title = tags.title?.takeIf { it.isNotBlank() }?.take(TrackRowMapper.MAX_TEXT) ?: row.title,
        artist = tags.artist?.takeIf { it.isNotBlank() }?.take(TrackRowMapper.MAX_TEXT) ?: row.artist,
        album = tags.album?.takeIf { it.isNotBlank() }?.take(TrackRowMapper.MAX_TEXT) ?: row.album,
        durationMs = tags.durationMs ?: row.durationMs,
        bitrateKbps = tags.bitrateKbps ?: row.bitrateKbps,
        sampleRateHz = tags.sampleRateHz ?: row.sampleRateHz,
    )
}
