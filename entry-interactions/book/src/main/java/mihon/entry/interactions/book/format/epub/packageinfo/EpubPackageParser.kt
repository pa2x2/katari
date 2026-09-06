package mihon.entry.interactions.book.format.epub.packageinfo

import mihon.entry.interactions.book.format.epub.EpubContract
import mihon.entry.interactions.book.format.epub.archive.EpubArchive
import mihon.entry.interactions.book.format.epub.archive.normalizeArchivePath
import mihon.entry.interactions.book.format.epub.xml.EPUB_CONTAINER_NAMESPACE
import mihon.entry.interactions.book.format.epub.xml.EPUB_METADATA_NAMESPACE
import mihon.entry.interactions.book.format.epub.xml.EPUB_PACKAGE_NAMESPACE
import mihon.entry.interactions.book.format.epub.xml.hasEpubXmlName
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
        val manifest = parseEpubManifest(archive, packageDocument, packageResource)
        val spine = packageDocument.getAllElements().firstOrNull { it.hasEpubXmlName("spine", EPUB_PACKAGE_NAMESPACE) }
            ?: error("Publication package has no spine")
        val documents = spine.children()
            .filter { it.hasEpubXmlName("itemref", EPUB_PACKAGE_NAMESPACE) }
            .map { itemRef ->
                val id = itemRef.attr("idref").trim()
                requireNotNull(manifest[id]) { "Publication spine references missing manifest item $id" }
            }
        val readingOrderIds = spine.children()
            .filter { it.hasEpubXmlName("itemref", EPUB_PACKAGE_NAMESPACE) && !it.attr("linear").equals("no", true) }
            .map { it.attr("idref").trim() }
            .toSet()
        val readingOrder = documents.filter { it.id in readingOrderIds }
        require(readingOrder.isNotEmpty()) { "Publication has no linear reading order" }
        require(readingOrder.all { !it.isRemote && it.mediaType in DOCUMENT_MEDIA_TYPES }) {
            "Publication contains an unsupported required reading-order resource"
        }
        val legacyNavigationId = spine.attr("toc").trim().takeIf(String::isNotEmpty)
        val uniqueIdentifier = packageDocument.getAllElements()
            .firstOrNull { it.hasEpubXmlName("package", EPUB_PACKAGE_NAMESPACE) }
            ?.attr("unique-identifier")
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { identifierId ->
                packageDocument.getAllElements()
                    .firstOrNull {
                        it.hasEpubXmlName("identifier", EPUB_METADATA_NAMESPACE) &&
                            it.attr("id") == identifierId
                    }
                    ?.text()
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
            }
        val resourceProtectionAlgorithms = if (archive.contains(ENCRYPTION_RESOURCE)) {
            parseEpubResourceProtectionAlgorithms(parseXml(ENCRYPTION_RESOURCE))
        } else {
            emptyMap()
        }
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
                .filter { it.hasEpubXmlName("language", EPUB_METADATA_NAMESPACE) }
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
            .firstOrNull { it.hasEpubXmlName("rootfile", EPUB_CONTAINER_NAMESPACE) }
            ?.attr("full-path")
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: error("Publication container does not declare a package document")
        return normalizeArchivePath(declared)
    }

    private fun rejectFixedLayout(document: Element) {
        val values = document.getAllElements().filter {
            it.hasEpubXmlName("meta", EPUB_PACKAGE_NAMESPACE)
        }.flatMap { meta ->
            listOfNotNull(
                meta.takeIf { it.attr("property") == "rendition:layout" }?.text(),
                meta.takeIf { it.attr("name") == "rendition:layout" }?.attr("content"),
            )
        }
        require(values.none { it.trim().equals("pre-paginated", true) }) {
            "Fixed-layout publications are not supported"
        }
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
