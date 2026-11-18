package com.canhamp.ui.library

import com.canhamp.data.db.GroupRow
import com.canhamp.data.db.SafFolderEntity
import com.canhamp.data.db.TrackEntity
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

enum class LibraryTab { ALL, FOLDERS, ARTISTS, ALBUMS, RECENT }

enum class PermissionState { Unknown, Needed, PermanentlyDenied, Granted }

/** Where the track list comes from; `pattern` is a LIKE pattern, "" for no filter. */
sealed interface QueryKind {
    data class All(val pattern: String) : QueryKind
    data class Recent(val pattern: String) : QueryKind
    data class Folder(val name: String, val pattern: String) : QueryKind
    data class Artist(val name: String, val pattern: String) : QueryKind
    data class Album(val name: String, val pattern: String) : QueryKind

    /** A group tab with nothing selected: the list shows group cards instead of tracks. */
    data class Groups(val tab: LibraryTab) : QueryKind
}

fun sourceFor(tab: LibraryTab, selection: String?, pattern: String): QueryKind = when (tab) {
    LibraryTab.ALL -> QueryKind.All(pattern)
    LibraryTab.RECENT -> QueryKind.Recent(pattern)
    LibraryTab.FOLDERS -> selection?.let { QueryKind.Folder(it, pattern) } ?: QueryKind.Groups(tab)
    LibraryTab.ARTISTS -> selection?.let { QueryKind.Artist(it, pattern) } ?: QueryKind.Groups(tab)
    LibraryTab.ALBUMS -> selection?.let { QueryKind.Album(it, pattern) } ?: QueryKind.Groups(tab)
}

/** Group cards whose name contains [query], ignoring case; a blank query keeps all. */
fun filterGroups(groups: List<GroupRow>, query: String): List<GroupRow> =
    if (query.isBlank()) groups else groups.filter { it.name.contains(query, ignoreCase = true) }

/** True for a Folders group that lives under an imported SAF folder (path "/SAF/<name>[/sub]"). */
fun isSafGroup(groupName: String): Boolean = groupName.startsWith("/SAF/")

/**
 * The imported folder a Folders group belongs to, or null for a MediaStore folder. Two imports with the
 * same display name share a prefix; the earlier one is returned.
 */
fun safFolderFor(groupName: String, folders: List<SafFolderEntity>): SafFolderEntity? {
    if (!isSafGroup(groupName)) return null
    return folders.firstOrNull { f ->
        val root = com.canhamp.data.library.SafTrackMapper.folderPath(f.displayName, "")
        groupName == root || groupName.startsWith("$root/")
    }
}

/** After the system dialog answered: a refusal without a rationale means "don't ask again" (B1). */
fun permissionAfterRequest(granted: Boolean, shouldShowRationale: Boolean): PermissionState = when {
    granted -> PermissionState.Granted
    shouldShowRationale -> PermissionState.Needed
    else -> PermissionState.PermanentlyDenied
}

enum class ListMode { Hidden, Skeleton, Empty, NoMatch, Populated }

/** Picks which body the list area shows. */
fun listMode(
    permission: PermissionState,
    scanning: Boolean,
    totalTracks: Int,
    everScanned: Boolean,
    queryActive: Boolean,
    listLoaded: Boolean,
    visibleCount: Int,
): ListMode = when {
    permission == PermissionState.Needed || permission == PermissionState.PermanentlyDenied -> ListMode.Hidden
    permission == PermissionState.Unknown -> ListMode.Skeleton
    totalTracks == 0 -> if (scanning || !everScanned) ListMode.Skeleton else ListMode.Empty
    listLoaded && visibleCount == 0 -> if (queryActive) ListMode.NoMatch else ListMode.Empty
    else -> ListMode.Populated
}

/** UI model of one track row. */
data class TrackRow(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val format: String,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,
    val sizeBytes: Long,
    val durationMs: Long,
    val dateModifiedSec: Long,
    val uri: String,
)

fun TrackEntity.toRow(): TrackRow = TrackRow(
    id = id,
    title = title,
    artist = artist,
    album = album,
    format = format,
    bitrateKbps = bitrateKbps,
    sampleRateHz = sampleRateHz,
    sizeBytes = sizeBytes,
    durationMs = durationMs,
    dateModifiedSec = dateModifiedSec,
    uri = uri,
)

/** "9.4 MB" style size, 1024-based. */
fun formatSize(bytes: Long): String {
    val b = bytes.coerceAtLeast(0)
    return when {
        b < 1024 -> "$b B"
        b < 1024L * 1024 -> String.format(Locale.US, "%.1f KB", b / 1024.0)
        b < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", b / (1024.0 * 1024))
        else -> String.format(Locale.US, "%.1f GB", b / (1024.0 * 1024 * 1024))
    }
}

/** 44100 -> "44.1", 48000 -> "48"; null for unknown. */
fun formatKhz(hz: Int?): String? {
    if (hz == null || hz <= 0) return null
    val tenths = (hz + 50) / 100
    return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
}

/** ISO date (yyyy-MM-dd) of an epoch-seconds stamp; null for unknown. */
fun formatDate(epochSec: Long, zone: ZoneId = ZoneId.systemDefault()): String? =
    if (epochSec <= 0) null else Instant.ofEpochSecond(epochSec).atZone(zone).toLocalDate().toString()

/** ".MP3 · 320 KBPS · 44.1 KHZ · 9.4 MB"; unknown parts are skipped. */
fun trackBadges(row: TrackRow): String = listOfNotNull(
    row.format.takeIf { it.isNotBlank() }?.let { ".${it.uppercase(Locale.ROOT)}" },
    row.bitrateKbps?.takeIf { it > 0 }?.let { "$it KBPS" },
    formatKhz(row.sampleRateHz)?.let { "$it KHZ" },
    formatSize(row.sizeBytes),
).joinToString(" · ")

/** Shortens "/storage/emulated/0/Music/x" to "/0/Music/x" for display. */
fun shortPath(path: String): String = path.removePrefix("/storage/emulated")
