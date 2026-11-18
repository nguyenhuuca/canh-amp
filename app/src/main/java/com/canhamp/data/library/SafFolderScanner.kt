package com.canhamp.data.library

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

/** One child of a document directory. */
data class DocEntry(
    val documentId: String,
    val name: String,
    val mime: String?,
    val size: Long,
    val lastModified: Long,
    val isDir: Boolean,
)

/** A granted document tree; the Android implementation reads it with [DocumentsContract]. */
interface DocumentTree {
    val rootDocumentId: String
    fun displayName(): String
    fun children(documentId: String): List<DocEntry>
    fun documentUri(documentId: String): String
}

/** Opens a tree by its URI string; throws [IllegalArgumentException] when the URI is not a tree. */
interface DocumentTrees {
    fun open(treeUri: String): DocumentTree
}

/** An audio file found by the walk; [relativeDir] has no leading or trailing slash ("" at the root). */
data class SafFile(
    val documentId: String,
    val uri: String,
    val name: String,
    val relativeDir: String,
    val mime: String?,
    val size: Long,
    val lastModified: Long,
)

/** Breadth-first walk of a [DocumentTree] that keeps supported audio files only (B10). */
class SafFolderScanner(private val tree: DocumentTree) {

    /** Returns the audio files found; [onProgress] gets the running file count. */
    fun scan(onProgress: (Int) -> Unit = {}): List<SafFile> {
        val found = ArrayList<SafFile>()
        val queue = ArrayDeque<Triple<String, String, Int>>()
        queue.add(Triple(tree.rootDocumentId, "", 0))
        var examined = 0
        while (queue.isNotEmpty() && found.size < MAX_FILES && examined < MAX_ENTRIES) {
            val (dirId, relDir, depth) = queue.removeFirst()
            for (child in tree.children(dirId)) {
                if (++examined > MAX_ENTRIES) break
                if (child.isDir) {
                    if (depth < MAX_DEPTH) queue.add(Triple(child.documentId, joinPath(relDir, child.name), depth + 1))
                } else if (isAudioName(child.name)) {
                    if (found.size >= MAX_FILES) break
                    found.add(
                        SafFile(
                            documentId = child.documentId,
                            uri = tree.documentUri(child.documentId),
                            name = child.name,
                            relativeDir = relDir,
                            mime = child.mime,
                            size = child.size.coerceAtLeast(0L),
                            lastModified = child.lastModified.coerceAtLeast(0L),
                        ),
                    )
                }
            }
            onProgress(found.size)
        }
        return found
    }

    companion object {
        const val MAX_DEPTH = 8
        const val MAX_FILES = 10_000

        /** Bound on directory entries looked at, so a huge tree without audio still ends. */
        const val MAX_ENTRIES = 200_000

        val EXTENSIONS = setOf("mp3", "flac", "ogg", "opus", "m4a", "aac", "wav")

        fun isAudioName(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in EXTENSIONS

        private fun joinPath(dir: String, name: String): String {
            val clean = name.replace('/', '_')
            return if (dir.isEmpty()) clean else "$dir/$clean"
        }
    }
}

/** [DocumentTree] over a content resolver. */
class AndroidDocumentTree(private val resolver: ContentResolver, private val treeUri: Uri) : DocumentTree {

    override val rootDocumentId: String = DocumentsContract.getTreeDocumentId(treeUri)

    override fun displayName(): String {
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId)
        resolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getString(0).orEmpty()
        }
        return ""
    }

    override fun children(documentId: String): List<DocEntry> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val out = ArrayList<DocEntry>()
        resolver.query(uri, columns, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val mime = c.getString(2)
                out.add(
                    DocEntry(
                        documentId = c.getString(0) ?: continue,
                        name = c.getString(1) ?: continue,
                        mime = mime,
                        size = if (c.isNull(3)) 0L else c.getLong(3),
                        lastModified = if (c.isNull(4)) 0L else c.getLong(4),
                        isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR,
                    ),
                )
            }
        }
        return out
    }

    override fun documentUri(documentId: String): String =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId).toString()
}

class AndroidDocumentTrees(private val resolver: ContentResolver) : DocumentTrees {
    override fun open(treeUri: String): DocumentTree {
        val uri = Uri.parse(treeUri)
        require(DocumentsContract.isTreeUri(uri)) { "not a document tree" }
        return AndroidDocumentTree(resolver, uri)
    }
}
