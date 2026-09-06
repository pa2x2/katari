package mihon.entry.interactions.book.format.epub.packageinfo

import mihon.entry.interactions.book.format.epub.archive.normalizeArchivePath
import mihon.entry.interactions.book.format.epub.xml.EPUB_ENCRYPTION_NAMESPACE
import mihon.entry.interactions.book.format.epub.xml.hasEpubXmlName
import org.jsoup.nodes.Element

internal fun parseEpubResourceProtectionAlgorithms(document: Element): Map<String, String> {
    return document.getAllElements()
        .filter { it.hasEpubXmlName("encrypteddata", EPUB_ENCRYPTION_NAMESPACE) }
        .mapNotNull { encrypted ->
            val algorithm = encrypted.getAllElements()
                .firstOrNull { it.hasEpubXmlName("encryptionmethod", EPUB_ENCRYPTION_NAMESPACE) }
                ?.attr("Algorithm")
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: return@mapNotNull null
            val uri = encrypted.getAllElements()
                .firstOrNull { it.hasEpubXmlName("cipherreference", EPUB_ENCRYPTION_NAMESPACE) }
                ?.attr("URI")
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: return@mapNotNull null
            val parsed = runCatching { java.net.URI(uri) }.getOrNull() ?: return@mapNotNull null
            require(!parsed.isAbsolute && parsed.rawPath.isNotBlank()) {
                "Publication encryption reference must identify a contained resource"
            }
            normalizeArchivePath(parsed.path.removePrefix("/")) to algorithm
        }
        .toMap()
}
