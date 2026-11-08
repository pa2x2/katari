package mihon.video.engine.spi.loading

import android.net.Uri
import android.provider.DocumentsContract

/**
 * The document [name] next to [document] in its storage tree, or null when [document] isn't a tree document. A
 * downloaded HLS episode is one folder whose playlists name their segments, keys and variant playlists by file name,
 * so each of those is a sibling of the playlist that names it.
 */
fun siblingDocumentUri(document: Uri, name: String): Uri? {
    if (document.scheme != "content") return null
    val documentId = runCatching { DocumentsContract.getDocumentId(document) }.getOrNull() ?: return null
    val parentId = documentId.substringBeforeLast('/', missingDelimiterValue = "")
    if (parentId.isBlank()) return null
    return runCatching { DocumentsContract.buildDocumentUriUsingTree(document, "$parentId/$name") }.getOrNull()
}
