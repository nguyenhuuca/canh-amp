package com.canhamp.data.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads tags and stream info with [MediaMetadataRetriever]; any failure is an unreadable file (B13). */
class TrackMetadataReader(private val context: Context) : TagSource {

    override suspend fun read(uri: String): TrackTags? = withContext(Dispatchers.IO) {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, Uri.parse(uri))
            fun s(key: Int) = TrackDetailsFormat.capTag(mmr.extractMetadata(key))
            fun n(key: Int) = s(key)?.toLongOrNull()?.takeIf { it > 0 }
            TrackTags(
                title = s(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = s(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = s(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                year = s(MediaMetadataRetriever.METADATA_KEY_YEAR),
                genre = s(MediaMetadataRetriever.METADATA_KEY_GENRE),
                trackNumber = s(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER),
                mimeType = s(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
                bitrateKbps = n(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.let { (it / BPS_PER_KBPS).toInt() },
                sampleRateHz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    n(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toInt()
                } else {
                    null
                },
                durationMs = n(MediaMetadataRetriever.METADATA_KEY_DURATION),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } finally {
            try {
                mmr.release()
            } catch (_: Exception) {
                // already released or never opened
            }
        }
    }

    private companion object {
        const val BPS_PER_KBPS = 1000L
    }
}
