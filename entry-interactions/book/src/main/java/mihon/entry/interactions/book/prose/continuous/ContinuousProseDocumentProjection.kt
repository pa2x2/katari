package mihon.entry.interactions.book.prose.continuous

import android.graphics.Typeface
import android.net.Uri
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mihon.entry.interactions.book.document.model.BookDocumentAlignment
import mihon.entry.interactions.book.document.model.BookDocumentBlock
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentBorderStyle
import mihon.entry.interactions.book.document.model.BookDocumentFontFamily
import mihon.entry.interactions.book.document.model.BookDocumentInlineStyle
import mihon.entry.interactions.book.document.model.BookDocumentLink
import mihon.entry.interactions.book.document.model.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.model.BookDocumentStyle
import mihon.entry.interactions.book.document.model.BookDocumentWhiteSpace
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildWindow
import tachiyomi.domain.entry.model.EntryChapter
import java.security.MessageDigest

internal const val CONTINUOUS_PROSE_ORIGIN = "https://reader.katari.invalid"

internal data class ContinuousProseProjection(
    val generation: Long,
    val currentSectionKey: String,
    val json: String,
    val resources: Map<String, ContinuousProseResourceSpec>,
    val blockLengths: Map<String, Map<String, Int>>,
)

internal data class ContinuousProseResourceSpec(
    val sectionKey: String,
    val resourceId: String,
    val kind: ContinuousProseResourceKind,
)

internal enum class ContinuousProseResourceKind {
    IMAGE,
    FONT,
}

internal sealed interface ContinuousProseTransitionState {
    data object Idle : ContinuousProseTransitionState

    data object Loading : ContinuousProseTransitionState

    data class Failed(val message: String) : ContinuousProseTransitionState
}

internal data class ContinuousProseTransitionLabels(
    val noPrevious: String,
    val noNext: String,
    val loading: String,
    val retry: String,
    val loadFailed: String,
)

internal fun buildContinuousProseProjection(
    generation: Long,
    window: EntryChildWindow<EntryChapter>,
    loaded: Map<Long, BookDocumentSection<EntryChapter>>,
    labels: ContinuousProseTransitionLabels,
    transitionStates: Map<Long, ContinuousProseTransitionState> = emptyMap(),
): ContinuousProseProjection {
    val resources = linkedMapOf<String, ContinuousProseResourceSpec>()
    val items = buildList {
        window.previous?.let { chapter ->
            loaded[chapter.id]?.let { add(it.toJson(resources)) }
        }
        add(window.previousTransition().toJson(labels, transitionStates))
        add(requireNotNull(loaded[window.current.id]).toJson(resources))
        add(window.nextTransition().toJson(labels, transitionStates))
        window.next?.let { chapter ->
            loaded[chapter.id]?.let { add(it.toJson(resources)) }
        }
    }
    val root = JsonObject(
        mapOf(
            "generation" to JsonPrimitive(generation),
            "currentSectionKey" to JsonPrimitive(window.current.id.toString()),
            "items" to JsonArray(items),
            "fonts" to JsonArray(
                resources
                    .filterValues { it.kind == ContinuousProseResourceKind.FONT }
                    .keys
                    .map { token ->
                        JsonObject(
                            mapOf(
                                "family" to JsonPrimitive("katari-$token"),
                                "source" to JsonPrimitive("$CONTINUOUS_PROSE_ORIGIN/resource/$token"),
                            ),
                        )
                    },
            ),
        ),
    )
    return ContinuousProseProjection(
        generation = generation,
        currentSectionKey = window.current.id.toString(),
        json = root.toString(),
        resources = resources,
        blockLengths = loaded.values.associate { section ->
            section.key to section.document.document.blocks.associate {
                it.id.value to it.logicalLength
            }
        },
    )
}

private fun BookDocumentSection<EntryChapter>.toJson(
    resources: MutableMap<String, ContinuousProseResourceSpec>,
): JsonObject {
    val projectedBlocks = JsonArray(document.blocks.map { it.toJson(key, resources) })
    val anchors = document.document.anchors.mapValues { (_, position) ->
        JsonObject(
            mapOf(
                "blockId" to JsonPrimitive(position.blockId.value),
                "offset" to JsonPrimitive(position.offsetWithinBlock),
            ),
        )
    }
    return JsonObject(
        mapOf(
            "type" to JsonPrimitive("section"),
            "key" to JsonPrimitive(key),
            "title" to JsonPrimitive(owner.name),
            "resourceId" to JsonPrimitive(document.document.resourceId),
            "revision" to document.document.revision.json(),
            "signature" to JsonPrimitive(
                document.document.revision
                    ?: MessageDigest.getInstance("SHA-256")
                        .digest(projectedBlocks.toString().toByteArray())
                        .take(16)
                        .joinToString("") { "%02x".format(it) },
            ),
            "logicalExtent" to JsonPrimitive(document.document.logicalExtent),
            "anchors" to JsonObject(anchors),
            "blocks" to projectedBlocks,
        ),
    )
}

private fun mihon.entry.interactions.viewer.EntryChildTransition<EntryChapter>.toJson(
    labels: ContinuousProseTransitionLabels,
    transitionStates: Map<Long, ContinuousProseTransitionState>,
): JsonObject {
    val loadState = to?.id?.let(transitionStates::get) ?: ContinuousProseTransitionState.Idle
    val transitionKey = to?.let { destination ->
        "transition:${listOf(from.id, destination.id).sorted().joinToString(":")}"
    } ?: "transition:${direction.name.lowercase()}:${from.id}:terminal"
    return JsonObject(
        mapOf(
            "type" to JsonPrimitive("transition"),
            "key" to JsonPrimitive(transitionKey),
            "direction" to JsonPrimitive(direction.name.lowercase()),
            "fromKey" to JsonPrimitive(from.id.toString()),
            "fromTitle" to JsonPrimitive(from.name),
            "toKey" to to?.id?.toString().json(),
            "toTitle" to to?.name.json(),
            "label" to JsonPrimitive(
                when (direction) {
                    EntryChildDirection.PREVIOUS -> to?.name ?: labels.noPrevious
                    EntryChildDirection.NEXT -> to?.name ?: labels.noNext
                },
            ),
            "loadingLabel" to JsonPrimitive(labels.loading),
            "retryLabel" to JsonPrimitive(labels.retry),
            "loadFailedLabel" to JsonPrimitive(labels.loadFailed),
            "loadState" to JsonPrimitive(
                when (loadState) {
                    ContinuousProseTransitionState.Idle -> "idle"
                    ContinuousProseTransitionState.Loading -> "loading"
                    is ContinuousProseTransitionState.Failed -> "failed"
                },
            ),
            "message" to (loadState as? ContinuousProseTransitionState.Failed)?.message.json(),
        ),
    )
}

private fun PreparedBookDocumentBlock.toJson(
    sectionKey: String,
    resources: MutableMap<String, ContinuousProseResourceSpec>,
    locatorBlockId: String? = null,
    locatorOffsetBase: Int = 0,
    locatorLogicalStart: Int? = null,
): JsonObject {
    val semantic = block.content
    val content = when (semantic) {
        is BookDocumentBlockContent.Text -> JsonObject(
            mapOf(
                "kind" to JsonPrimitive("text"),
                "text" to JsonPrimitive(displayText()),
            ),
        )
        is BookDocumentBlockContent.ListBlock -> JsonObject(
            mapOf(
                "kind" to JsonPrimitive("list"),
                "ordered" to JsonPrimitive(semantic.ordered),
                "start" to JsonPrimitive(semantic.start),
                "items" to listItemsJson(
                    semantic = semantic,
                    displayText = displayText(),
                    sectionKey = sectionKey,
                    resources = resources,
                ),
            ),
        )
        is BookDocumentBlockContent.Figure -> {
            val token = resources.register(sectionKey, semantic.image.resourceId, ContinuousProseResourceKind.IMAGE)
            JsonObject(
                mapOf(
                    "kind" to JsonPrimitive("figure"),
                    "source" to JsonPrimitive("$CONTINUOUS_PROSE_ORIGIN/resource/$token"),
                    "alternativeText" to semantic.image.alternativeText.json(),
                    "width" to semantic.image.width.json(),
                    "height" to semantic.image.height.json(),
                    "caption" to semantic.caption.json(),
                ),
            )
        }
        is BookDocumentBlockContent.Table -> JsonObject(
            mapOf(
                "kind" to JsonPrimitive("table"),
                "caption" to semantic.caption.json(),
                "captionLinks" to semantic.captionLinks.linksJson(),
                "rows" to JsonArray(
                    semantic.rows.map { row ->
                        JsonArray(
                            row.cells.map { cell ->
                                JsonObject(
                                    mapOf(
                                        "text" to JsonPrimitive(cell.text),
                                        "header" to JsonPrimitive(cell.header),
                                        "columnSpan" to JsonPrimitive(cell.columnSpan),
                                        "rowSpan" to JsonPrimitive(cell.rowSpan),
                                        "links" to cell.links.linksJson(),
                                    ),
                                )
                            },
                        )
                    },
                ),
            ),
        )
        is BookDocumentBlockContent.Disclosure -> JsonObject(
            mapOf(
                "kind" to JsonPrimitive("disclosure"),
                "summary" to JsonPrimitive(semantic.summary),
                "expanded" to JsonPrimitive(semantic.initiallyExpanded),
                "body" to JsonArray(
                    disclosureBody.map {
                        it.toJson(
                            sectionKey = sectionKey,
                            resources = resources,
                            locatorBlockId = block.id.value,
                            locatorOffsetBase = locatorOffsetBase +
                                semantic.summary.length +
                                1 +
                                it.block.logicalStart,
                            locatorLogicalStart = locatorLogicalStart ?: block.logicalStart,
                        )
                    },
                ),
            ),
        )
        BookDocumentBlockContent.ThematicBreak -> JsonObject(mapOf("kind" to JsonPrimitive("break")))
        is BookDocumentBlockContent.Unsupported -> JsonObject(
            mapOf(
                "kind" to JsonPrimitive("unsupported"),
                "text" to JsonPrimitive("-- unsupported content block --"),
            ),
        )
    }
    block.style.fontFamily.register(sectionKey, resources)
    block.inlineStyles.forEach { it.style.fontFamily.register(sectionKey, resources) }
    return JsonObject(
        mapOf(
            "id" to JsonPrimitive(block.id.value),
            "role" to JsonPrimitive(block.role.kind.name.lowercase()),
            "level" to block.role.level.json(),
            "logicalStart" to JsonPrimitive(block.logicalStart),
            "logicalLength" to JsonPrimitive(block.logicalLength),
            "locatorBlockId" to JsonPrimitive(locatorBlockId ?: block.id.value),
            "locatorOffsetBase" to JsonPrimitive(locatorOffsetBase),
            "locatorLogicalStart" to JsonPrimitive(locatorLogicalStart ?: block.logicalStart),
            "style" to block.style.toJson(sectionKey, resources),
            "links" to block.links.linksJson(),
            "inlineStyles" to JsonArray(
                projectedInlineStylesJson(
                    sectionKey = sectionKey,
                    resources = resources,
                    start = 0,
                    endExclusive = displayText().length,
                ),
            ),
            "content" to content,
        ),
    )
}

private fun PreparedBookDocumentBlock.listItemsJson(
    semantic: BookDocumentBlockContent.ListBlock,
    displayText: String,
    sectionKey: String,
    resources: MutableMap<String, ContinuousProseResourceSpec>,
): JsonArray {
    var cursor = 0
    return JsonArray(
        semantic.items.map { item ->
            val marker = item.marker ?: "•"
            val prefixLength = item.depth * 2 + marker.length + 1
            val itemStart = (cursor + prefixLength).coerceAtMost(displayText.length)
            val itemEnd = (itemStart + item.text.length).coerceAtMost(displayText.length)
            cursor = (itemEnd + 1).coerceAtMost(displayText.length)
            JsonObject(
                mapOf(
                    "text" to JsonPrimitive(displayText.substring(itemStart, itemEnd)),
                    "depth" to JsonPrimitive(item.depth),
                    "marker" to JsonPrimitive(marker),
                    "links" to block.links
                        .linksWithin(itemStart, itemEnd)
                        .linksJson(),
                    "inlineStyles" to JsonArray(
                        projectedInlineStylesJson(
                            sectionKey = sectionKey,
                            resources = resources,
                            start = itemStart,
                            endExclusive = itemEnd,
                        ),
                    ),
                ),
            )
        },
    )
}

/**
 * Structural paragraph terminators belong to the document model, but not to the browser selection surface.
 * Visual separation is supplied by CSS, so selecting adjacent blocks never includes a synthetic empty line.
 */
internal fun PreparedBookDocumentBlock.displayText(): String =
    renderedText.toString().removeSuffix("\n\n")

private fun PreparedBookDocumentBlock.projectedInlineStylesJson(
    sectionKey: String,
    resources: MutableMap<String, ContinuousProseResourceSpec>,
    start: Int,
    endExclusive: Int,
): List<JsonObject> {
    if (endExclusive <= start) return emptyList()
    val boundaries = sortedSetOf(start, endExclusive)
    block.inlineStyles.forEach { range ->
        if (range.start < endExclusive && range.endExclusive > start) {
            boundaries += range.start.coerceIn(start, endExclusive)
            boundaries += range.endExclusive.coerceIn(start, endExclusive)
        }
    }
    renderedText.getSpans(start, endExclusive, Any::class.java).forEach { span ->
        val spanStart = renderedText.getSpanStart(span)
        val spanEnd = renderedText.getSpanEnd(span)
        if (spanStart < endExclusive && spanEnd > start) {
            boundaries += spanStart.coerceIn(start, endExclusive)
            boundaries += spanEnd.coerceIn(start, endExclusive)
        }
    }
    return boundaries.zipWithNext().mapNotNull { (segmentStart, segmentEnd) ->
        if (segmentEnd <= segmentStart) return@mapNotNull null
        val style = ProjectedInlineStyle()
        block.inlineStyles
            .filter { it.start <= segmentStart && segmentStart < it.endExclusive }
            .forEach { style.merge(it.style) }
        renderedText.getSpans(segmentStart, segmentEnd, Any::class.java).forEach(style::merge)
        if (style.isEmpty()) {
            null
        } else {
            JsonObject(
                mapOf(
                    "start" to JsonPrimitive(segmentStart - start),
                    "end" to JsonPrimitive(segmentEnd - start),
                    "style" to style.toJson(sectionKey, resources),
                ),
            )
        }
    }
}

private data class ProjectedInlineStyle(
    var foregroundArgb: Long? = null,
    var backgroundArgb: Long? = null,
    var fontFamily: BookDocumentFontFamily? = null,
    var platformFontFamily: String? = null,
    var fontSizeScale: Float? = null,
    var bold: Boolean = false,
    var italic: Boolean = false,
) {
    fun merge(style: BookDocumentInlineStyle) {
        foregroundArgb = style.foregroundArgb ?: foregroundArgb
        backgroundArgb = style.backgroundArgb ?: backgroundArgb
        fontFamily = style.fontFamily ?: fontFamily
        fontSizeScale = style.fontSizeScale ?: fontSizeScale
        bold = bold || style.bold
    }

    fun merge(span: Any) {
        when (span) {
            is ForegroundColorSpan -> foregroundArgb = span.foregroundColor.toLong() and 0xffff_ffffL
            is BackgroundColorSpan -> backgroundArgb = span.backgroundColor.toLong() and 0xffff_ffffL
            is RelativeSizeSpan -> fontSizeScale = (fontSizeScale ?: 1f) * span.sizeChange
            is TypefaceSpan -> platformFontFamily = span.family
            is StyleSpan -> {
                bold = bold || span.style == Typeface.BOLD || span.style == Typeface.BOLD_ITALIC
                italic = italic || span.style == Typeface.ITALIC || span.style == Typeface.BOLD_ITALIC
            }
        }
    }

    fun isEmpty(): Boolean =
        foregroundArgb == null &&
            backgroundArgb == null &&
            fontFamily == null &&
            platformFontFamily == null &&
            fontSizeScale == null &&
            !bold &&
            !italic

    fun toJson(
        sectionKey: String,
        resources: MutableMap<String, ContinuousProseResourceSpec>,
    ): JsonObject = JsonObject(
        mapOf(
            "foreground" to foregroundArgb.colorJson(),
            "background" to backgroundArgb.colorJson(),
            "fontSizeScale" to fontSizeScale.json(),
            "bold" to JsonPrimitive(bold),
            "italic" to JsonPrimitive(italic),
            "fontFamily" to (
                fontFamily?.toJson(sectionKey, resources)
                    ?: platformFontFamily
                        ?.takeIf { it in SUPPORTED_PLATFORM_FONT_FAMILIES }
                        ?.let(::JsonPrimitive)
                    ?: JsonNull
                ),
        ),
    )
}

private fun BookDocumentStyle.toJson(
    sectionKey: String,
    resources: MutableMap<String, ContinuousProseResourceSpec>,
): JsonObject = JsonObject(
    mapOf(
        "alignment" to alignment?.name?.lowercase().json(),
        "whiteSpace" to when (whiteSpace) {
            BookDocumentWhiteSpace.NORMAL -> JsonPrimitive("normal")
            BookDocumentWhiteSpace.PRE_WRAP -> JsonPrimitive("pre-wrap")
            BookDocumentWhiteSpace.PRE -> JsonPrimitive("pre")
        },
        "foreground" to foregroundArgb.colorJson(),
        "background" to backgroundArgb.colorJson(),
        "paddingEm" to JsonPrimitive(paddingEm),
        "fontSizeScale" to JsonPrimitive(fontSizeScale),
        "bold" to JsonPrimitive(bold),
        "fontFamily" to fontFamily.toJson(sectionKey, resources),
        "border" to (
            border?.let {
                JsonObject(
                    mapOf(
                        "width" to JsonPrimitive(it.widthDp),
                        "color" to it.colorArgb.colorJson(),
                        "style" to JsonPrimitive(
                            when (it.style) {
                                BookDocumentBorderStyle.SOLID -> "solid"
                                BookDocumentBorderStyle.DASHED -> "dashed"
                                BookDocumentBorderStyle.DOTTED -> "dotted"
                            },
                        ),
                    ),
                )
            } ?: JsonNull
            ),
    ),
)

private fun List<BookDocumentLink>.linksJson(): JsonArray = JsonArray(
    map { link ->
        JsonObject(
            mapOf(
                "start" to JsonPrimitive(link.start),
                "end" to JsonPrimitive(link.endExclusive),
                "targetType" to JsonPrimitive(
                    when (link.target) {
                        is BookDocumentLinkTarget.Anchor -> "anchor"
                        is BookDocumentLinkTarget.External -> "external"
                    },
                ),
                "target" to JsonPrimitive(
                    when (val target = link.target) {
                        is BookDocumentLinkTarget.Anchor -> target.fragment
                        is BookDocumentLinkTarget.External -> target.url
                    },
                ),
            ),
        )
    },
)

private fun List<BookDocumentLink>.linksWithin(
    start: Int,
    endExclusive: Int,
): List<BookDocumentLink> = mapNotNull { link ->
    val clippedStart = maxOf(link.start, start)
    val clippedEnd = minOf(link.endExclusive, endExclusive)
    if (clippedEnd <= clippedStart) {
        null
    } else {
        link.copy(
            start = clippedStart - start,
            endExclusive = clippedEnd - start,
        )
    }
}

private fun BookDocumentFontFamily?.register(
    sectionKey: String,
    resources: MutableMap<String, ContinuousProseResourceSpec>,
) {
    val resource = this as? BookDocumentFontFamily.Resource ?: return
    resources.register(sectionKey, resource.resourceId, ContinuousProseResourceKind.FONT)
}

private fun BookDocumentFontFamily?.toJson(
    sectionKey: String,
    resources: MutableMap<String, ContinuousProseResourceSpec>,
): JsonElement = when (this) {
    null -> JsonNull
    is BookDocumentFontFamily.Generic -> JsonPrimitive(
        when (family) {
            BookDocumentFontFamily.GenericFamily.SERIF -> "serif"
            BookDocumentFontFamily.GenericFamily.SANS_SERIF -> "sans-serif"
            BookDocumentFontFamily.GenericFamily.MONOSPACE -> "monospace"
        },
    )
    is BookDocumentFontFamily.Resource -> {
        val token = resources.register(sectionKey, resourceId, ContinuousProseResourceKind.FONT)
        JsonPrimitive("katari-$token")
    }
}

private fun MutableMap<String, ContinuousProseResourceSpec>.register(
    sectionKey: String,
    resourceId: String,
    kind: ContinuousProseResourceKind,
): String {
    val token = MessageDigest.getInstance("SHA-256")
        .digest("$sectionKey\u0000$resourceId\u0000${kind.name}".toByteArray())
        .take(16)
        .joinToString("") { "%02x".format(it) }
    put(token, ContinuousProseResourceSpec(sectionKey, resourceId, kind))
    return Uri.encode(token)
}

private fun Long?.colorJson(): JsonElement = this?.let { argb ->
    val alpha = (argb shr 24) and 0xff
    val red = (argb shr 16) and 0xff
    val green = (argb shr 8) and 0xff
    val blue = argb and 0xff
    JsonPrimitive("#%02x%02x%02x%02x".format(red, green, blue, alpha))
} ?: JsonNull

private fun Any?.json(): JsonElement = when (this) {
    null -> JsonNull
    is String -> JsonPrimitive(this)
    is Int -> JsonPrimitive(this)
    is Long -> JsonPrimitive(this)
    is Float -> JsonPrimitive(this)
    else -> error("Unsupported JSON primitive ${this::class}")
}

private val SUPPORTED_PLATFORM_FONT_FAMILIES = setOf("serif", "sans-serif", "monospace")
