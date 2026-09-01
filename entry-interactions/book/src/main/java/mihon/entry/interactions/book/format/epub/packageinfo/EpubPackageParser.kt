package mihon.entry.interactions.book.format.epub.packageinfo

import mihon.entry.interactions.book.format.epub.EpubContract
import mihon.entry.interactions.book.format.epub.archive.EpubArchive
import mihon.entry.interactions.book.format.epub.archive.EpubArchiveReference
import mihon.entry.interactions.book.format.epub.archive.normalizeArchivePath
import mihon.entry.interactions.book.format.epub.archive.resolveArchiveReference
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

internal class EpubPackageParser(
    private val archive: EpubArchive,
) {
    fun parse(): EpubPackage {
        val packageResource = parsePackageResource()
        val packageDocument = parseXml(packageResource)
        rejectFixedLayout(packageDocument)
        val manifest = parseManifest(packageDocument, packageResource)
        val spine = packageDocument.getAllElements().firstOrNull { it.normalName() == "spine" }
            ?: error("Publication package has no spine")
        val documents = spine.children()
            .filter { it.normalName() == "itemref" }
            .map { itemRef ->
                val id = itemRef.attr("idref").trim()
                requireNotNull(manifest[id]) { "Publication spine references missing manifest item $id" }
            }
        val readingOrderIds = spine.children()
            .filter { it.normalName() == "itemref" && !it.attr("linear").equals("no", true) }
            .map { it.attr("idref").trim() }
            .toSet()
        val readingOrder = documents.filter { it.id in readingOrderIds }
        require(readingOrder.isNotEmpty()) { "Publication has no linear reading order" }
        require(readingOrder.all { it.mediaType in DOCUMENT_MEDIA_TYPES }) {
            "Publication contains an unsupported required reading-order resource"
        }
        val legacyNavigationId = spine.attr("toc").trim().takeIf(String::isNotEmpty)
        val uniqueIdentifier = packageDocument.getAllElements()
            .firstOrNull { it.normalName() == "package" }
            ?.attr("unique-identifier")
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { identifierId ->
                packageDocument.getAllElements()
                    .firstOrNull { it.normalName() == "identifier" && it.attr("id") == identifierId }
                    ?.text()
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
            }
        val resourceProtectionAlgorithms = parseResourceProtectionAlgorithms()
        val requiredResources = buildSet {
            add(packageResource)
            addAll(readingOrder.map(EpubManifestItem::resourceId))
            manifest.values.firstOrNull { "nav" in it.properties }?.resourceId?.let(::add)
            legacyNavigationId?.let(manifest::get)?.resourceId?.let(::add)
        }
        require(requiredResources.none(resourceProtectionAlgorithms::containsKey)) {
            "Publication requires an encrypted reading resource"
        }
        return EpubPackage(
            packageResource = packageResource,
            manifest = manifest,
            documents = documents,
            readingOrder = readingOrder,
            navigationResource = manifest.values.firstOrNull { "nav" in it.properties },
            legacyNavigationResource = legacyNavigationId?.let(manifest::get)
                ?: manifest.values.firstOrNull { it.mediaType == NCX_MEDIA_TYPE },
            uniqueIdentifier = uniqueIdentifier,
            resourceProtectionAlgorithms = resourceProtectionAlgorithms,
            languages = packageDocument.getAllElements()
                .filter { it.normalName() == "language" }
                .map(Element::text)
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct(),
            rightToLeft = spine.attr("page-progression-direction").equals("rtl", true),
        )
    }

    private fun parsePackageResource(): String {
        val container = parseXml(CONTAINER_RESOURCE)
        val declared = container.getAllElements()
            .firstOrNull { it.normalName() == "rootfile" }
            ?.attr("full-path")
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: error("Publication container does not declare a package document")
        return normalizeArchivePath(declared)
    }

    private fun parseManifest(document: Element, packageResource: String): Map<String, EpubManifestItem> {
        val manifest = document.getAllElements().firstOrNull { it.normalName() == "manifest" }
            ?: error("Publication package has no manifest")
        return manifest.children().filter { it.normalName() == "item" }.associate { item ->
            val id = item.attr("id").trim()
            val href = item.attr("href").trim()
            val mediaType = item.attr("media-type").substringBefore(';').trim().lowercase()
            require(id.isNotEmpty() && href.isNotEmpty() && mediaType.isNotEmpty()) {
                "Publication manifest contains an incomplete item"
            }
            val reference = resolveArchiveReference(packageResource, href) as? EpubArchiveReference.Internal
                ?: error("Publication manifest item must use a contained relative resource")
            require(archive.contains(reference.path)) { "Publication manifest resource is missing: ${reference.path}" }
            id to EpubManifestItem(
                id = id,
                resourceId = reference.path,
                mediaType = mediaType,
                properties = item.attr("properties").split(Regex("\\s+")).filter(String::isNotEmpty).toSet(),
            )
        }
    }

    private fun rejectFixedLayout(document: Element) {
        val values = document.getAllElements().filter { it.normalName() == "meta" }.flatMap { meta ->
            listOfNotNull(
                meta.takeIf { it.attr("property") == "rendition:layout" }?.text(),
                meta.takeIf { it.attr("name") == "rendition:layout" }?.attr("content"),
            )
        }
        require(values.none { it.trim().equals("pre-paginated", true) }) {
            "Fixed-layout publications are not supported"
        }
    }

    private fun parseResourceProtectionAlgorithms(): Map<String, String> {
        if (!archive.contains(ENCRYPTION_RESOURCE)) return emptyMap()
        val document = parseXml(ENCRYPTION_RESOURCE)
        return document.getAllElements()
            .filter { it.normalName() == "encrypteddata" }
            .mapNotNull { encrypted ->
                val algorithm = encrypted.getAllElements()
                    .firstOrNull { it.normalName() == "encryptionmethod" }
                    ?.attr("Algorithm")
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?: return@mapNotNull null
                val uri = encrypted.getAllElements()
                    .firstOrNull { it.normalName() == "cipherreference" }
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

    private fun parseXml(resourceId: String): Element = Jsoup.parse(
        archive.readText(resourceId, EpubContract.MAX_XML_BYTES),
        "",
        Parser.xmlParser(),
    )

    private companion object {
        const val CONTAINER_RESOURCE = "META-INF/container.xml"
        const val ENCRYPTION_RESOURCE = "META-INF/encryption.xml"
        const val NCX_MEDIA_TYPE = "application/x-dtbncx+xml"
        val DOCUMENT_MEDIA_TYPES = setOf("application/xhtml+xml", "text/html")
    }
}
