package mihon.entry.interactions.book.format.epub.preparation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.book.api.document.BookDocument
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockId
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentContent
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.format.epub.EpubContract
import mihon.entry.interactions.book.format.epub.archive.EpubArchive
import mihon.entry.interactions.book.format.epub.archive.EpubArchiveReference
import mihon.entry.interactions.book.format.epub.archive.resolveArchiveReference
import mihon.entry.interactions.book.format.epub.packageinfo.EpubManifestItem
import mihon.entry.interactions.book.format.epub.packageinfo.EpubPackage
import mihon.entry.interactions.book.format.html.prosechapter.parsing.HtmlProseDocumentParser
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizationPolicy
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import mihon.entry.interactions.book.preparation.BookPublicationResource
import mihon.entry.interactions.book.preparation.BookRemoteResourceReference
import mihon.entry.interactions.book.preparation.BookRemoteResourceType
import org.jsoup.Jsoup
import java.security.MessageDigest

/** Converts one EPUB content document and its authored resource references into the canonical document model. */
internal class EpubDocumentPreparer {
    fun supports(mediaType: String): Boolean = mediaType in DOCUMENT_MEDIA_TYPES

    suspend fun prepare(
        archive: EpubArchive,
        packageInfo: EpubPackage,
        item: EpubManifestItem,
        revision: String,
        derivedResources: MutableMap<String, BookPublicationResource>,
        remoteResources: MutableMap<String, BookRemoteResourceReference>,
    ): EpubPreparedDocument = withContext(Dispatchers.Default) {
        val bytes = withContext(Dispatchers.IO) { archive.read(item.resourceId, EpubContract.MAX_DOCUMENT_BYTES) }
        val title = Jsoup.parse(bytes.toString(Charsets.UTF_8)).title().trim().takeIf(String::isNotEmpty)
        val linkedStyles = linkedStyleSheets(
            archive,
            packageInfo,
            item.resourceId,
            bytes,
            remoteResources,
        )
        val manifestByResource = packageInfo.manifest.values.associateBy(EpubManifestItem::resourceId)
        val body = HtmlProseSanitizer.sanitize(
            bytes,
            HtmlProseSanitizationPolicy(
                xmlSyntax = true,
                additionalStyleSheets = linkedStyles.css,
                resolveLink = { href -> resolveLink(item.resourceId, href, manifestByResource) },
                resolveImageResource = { source ->
                    resolveImage(item.resourceId, source, manifestByResource, remoteResources)
                },
                resolveInlineImage = { svg ->
                    val inlineBytes = svg.encodeToByteArray()
                    val resourceId = "katari-derived/vector/${inlineBytes.sha256()}.svg"
                    derivedResources.putIfAbsent(
                        resourceId,
                        BookPublicationResource(resourceId, "image/svg+xml", inlineBytes),
                    )
                    resourceId
                },
                resolveFontResource = { family ->
                    linkedStyles.fontResources[family.normalizedFontFamily()]
                },
            ),
        )
        val document = try {
            HtmlProseDocumentParser().parse(item.resourceId, revision, body)
        } catch (error: IllegalArgumentException) {
            if (error.message?.contains("no readable document blocks") != true) throw error
            unsupported(item.resourceId, revision)
        }
        EpubPreparedDocument(
            document = document,
            title = title,
            hasReadableContent = document.blocks.any { it.content !is BookDocumentBlockContent.Unsupported },
        )
    }

    fun unsupported(resourceId: String, revision: String): BookDocument {
        val text = "\uFFFC\n\n"
        return BookDocument(
            resourceId = resourceId,
            revision = revision,
            content = BookDocumentContent(
                text = text,
                blocks = listOf(
                    BookDocumentBlock(
                        id = BookDocumentBlockId("unsupported-publication-section"),
                        role = BookDocumentBlockRole(BookDocumentBlockKind.UNSUPPORTED),
                        content = BookDocumentBlockContent.Unsupported("publication section"),
                        plainText = "",
                        sourceFragments = emptyList(),
                        logicalStart = 0,
                        logicalEndExclusive = text.length,
                    ),
                ),
                anchors = emptyMap(),
            ),
        )
    }

    private suspend fun linkedStyleSheets(
        archive: EpubArchive,
        packageInfo: EpubPackage,
        documentResource: String,
        bytes: ByteArray,
        remoteResources: MutableMap<String, BookRemoteResourceReference>,
    ): EpubLinkedStyles {
        val manifestByResource = packageInfo.manifest.values.associateBy(EpubManifestItem::resourceId)
        val references = Jsoup.parse(bytes.toString(Charsets.UTF_8)).select("link[rel~=stylesheet][href]")
            .map { it.attr("href") }
        val fontResources = linkedMapOf<String, String>()
        val css = references.mapNotNull { href ->
            val reference = resolveArchiveReference(documentResource, href) as? EpubArchiveReference.Internal
                ?: return@mapNotNull null
            val item = manifestByResource[reference.path]?.takeIf { it.mediaType == "text/css" }
                ?: return@mapNotNull null
            val styleSheet = withContext(Dispatchers.IO) {
                archive.readText(item.resourceId, EpubContract.MAX_STYLE_SHEET_BYTES)
            }
            discoverFontResources(styleSheet, item.resourceId, manifestByResource, remoteResources, fontResources)
            styleSheet
        }
        return EpubLinkedStyles(css, fontResources)
    }

    private fun discoverFontResources(
        css: String,
        styleSheetResource: String,
        manifest: Map<String, EpubManifestItem>,
        remoteResources: MutableMap<String, BookRemoteResourceReference>,
        destination: MutableMap<String, String>,
    ) {
        FONT_FACE.findAll(css).forEach { match ->
            val declarations = match.groupValues[1]
            val family = FONT_FAMILY.find(declarations)?.groupValues?.get(1)?.normalizedFontFamily()
                ?: return@forEach
            val source = FONT_SOURCE.find(declarations)?.groupValues?.get(1)?.trim()?.trim('"', '\'')
                ?: return@forEach
            val resourceId = when (val reference = resolveArchiveReference(styleSheetResource, source)) {
                is EpubArchiveReference.Internal -> manifest[reference.path]
                    ?.takeIf { item -> item.mediaType in FONT_MEDIA_TYPES }
                    ?.resourceId
                is EpubArchiveReference.External -> reference.url.takeIf(::isHttpUrl)?.let { url ->
                    val id = "katari-remote/font/${url.encodeToByteArray().sha256()}"
                    remoteResources.putIfAbsent(
                        id,
                        BookRemoteResourceReference(id, url, BookRemoteResourceType.FONT),
                    )
                    id
                }
                null -> null
            }
            if (resourceId != null) destination.putIfAbsent(family, resourceId)
        }
    }

    private fun resolveLink(
        documentResource: String,
        href: String,
        manifest: Map<String, EpubManifestItem>,
    ): BookDocumentLinkTarget? = when (val reference = resolveArchiveReference(documentResource, href)) {
        is EpubArchiveReference.External -> reference.url.takeIf(::isHttpUrl)?.let(BookDocumentLinkTarget::External)
        is EpubArchiveReference.Internal -> {
            if (reference.path == documentResource && reference.fragment != null) {
                BookDocumentLinkTarget.Anchor(reference.fragment)
            } else {
                manifest[reference.path]
                    ?.takeIf { it.mediaType in DOCUMENT_MEDIA_TYPES }
                    ?.let { BookDocumentLinkTarget.Resource(reference.path, reference.fragment) }
            }
        }
        null -> null
    }

    private fun resolveImage(
        documentResource: String,
        source: String,
        manifest: Map<String, EpubManifestItem>,
        remoteResources: MutableMap<String, BookRemoteResourceReference>,
    ): String? = when (val reference = resolveArchiveReference(documentResource, source)) {
        is EpubArchiveReference.Internal ->
            manifest[reference.path]?.takeIf { it.mediaType.startsWith("image/") }?.resourceId
        is EpubArchiveReference.External -> reference.url.takeIf(::isHttpUrl)?.let { url ->
            val resourceId = "katari-remote/image/${url.encodeToByteArray().sha256()}"
            remoteResources.putIfAbsent(
                resourceId,
                BookRemoteResourceReference(resourceId, url, BookRemoteResourceType.IMAGE),
            )
            resourceId
        }
        null -> null
    }

    private companion object {
        val DOCUMENT_MEDIA_TYPES = setOf("application/xhtml+xml", "text/html")
        val FONT_MEDIA_TYPES = setOf(
            "font/ttf",
            "font/otf",
            "application/font-sfnt",
            "application/vnd.ms-opentype",
            "application/x-font-opentype",
            "application/x-font-ttf",
        )
        val FONT_FACE =
            Regex("@font-face\\s*\\{([^}]*)\\}", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val FONT_FAMILY = Regex("font-family\\s*:\\s*([^;]+)", RegexOption.IGNORE_CASE)
        val FONT_SOURCE = Regex("url\\(\\s*([^)]+)\\s*\\)", RegexOption.IGNORE_CASE)
        fun isHttpUrl(url: String) = url.startsWith("https://", true) || url.startsWith("http://", true)
    }
}

internal data class EpubPreparedDocument(
    val document: BookDocument,
    val title: String?,
    val hasReadableContent: Boolean,
)

private data class EpubLinkedStyles(
    val css: List<String>,
    val fontResources: Map<String, String>,
)

private fun String.normalizedFontFamily(): String = trim()
    .substringBefore(',')
    .trim()
    .trim('"', '\'')
    .lowercase()

private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(this)
    .joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }
