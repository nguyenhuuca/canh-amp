package com.canhamp.data.playlist

import android.content.ContentResolver
import android.net.Uri
import com.canhamp.data.db.TrackDao
import com.canhamp.data.db.TrackEntity
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

private const val CHUNK = 500

/** [PlaylistTrackIndex] over Room, in chunks that stay under SQLite's bound-variable limit. */
class TrackDaoPlaylistIndex(private val dao: TrackDao) : PlaylistTrackIndex {
    override suspend fun findByUris(uris: List<String>): List<TrackEntity> =
        uris.chunked(CHUNK).flatMap { dao.findByUris(it) }

    override suspend fun findByFilePaths(paths: List<String>): List<TrackEntity> =
        paths.chunked(CHUNK).flatMap { dao.findByFilePaths(it) }
}

/** [PlaylistStreams] over the system picker's document uris. */
class ContentResolverStreams(private val resolver: ContentResolver) : PlaylistStreams {
    override fun openInput(uri: String): InputStream? = try {
        resolver.openInputStream(Uri.parse(uri))
    } catch (_: FileNotFoundException) {
        null
    }

    override fun openOutput(uri: String): OutputStream? = try {
        resolver.openOutputStream(Uri.parse(uri), "wt")
    } catch (_: FileNotFoundException) {
        null
    }
}
