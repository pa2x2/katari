package mihon.entry.interactions.book.format.epub.packageinfo

import mihon.entry.interactions.book.format.epub.archive.EpubArchive
import mihon.entry.interactions.book.format.epub.archive.EpubArchiveReference
import mihon.entry.interactions.book.format.epub.archive.resolveArchiveReference
import mihon.entry.interactions.book.format.epub.xml.EPUB_PACKAGE_NAMESPACE
import mihon.entry.interactions.book.format.epub.xml.hasEpubXmlName
import org.jsoup.nodes.Element

internal fun parseEpubManifest(
    archive: EpubArchive,
    document: Element,
    packageResource: String,
): Map<String, EpubManifestItem> {
    val manifest = document.getAllElements().firstOrNull { it.hasEpubXmlName("manifest", EPUB_PACKAGE_NAMESPACE) }
        ?: error("Publication package has no manifest")
    return manifest.children().filter { it.hasEpubXmlName("item", EPUB_PACKAGE_NAMESPACE) }.associate { item ->
        val id = item.attr("id").trim()
        val href = item.attr("href").trim()
        val mediaType = item.attr("media-type").substringBefore(';').trim().lowercase()
        require(id.isNotEmpty() && href.isNotEmpty() && mediaType.isNotEmpty()) {
            "Publication manifest contains an incomplete item"
        }
        val reference = requireNotNull(resolveArchiveReference(packageResource, href)) {
            "Publication manifest item has an invalid resource reference"
        }
        val resourceId = when (reference) {
            is EpubArchiveReference.Internal -> {
                require(archive.contains(reference.path)) {
                    "Publication manifest resource is missing: ${reference.path}"
                }
                reference.path
            }
            is EpubArchiveReference.External -> reference.url
        }
        id to EpubManifestItem(
            id = id,
            resourceId = resourceId,
            isRemote = reference is EpubArchiveReference.External,
            mediaType = mediaType,
            properties = item.attr("properties").split(Regex("\\s+")).filter(String::isNotEmpty).toSet(),
        )
    }
}
