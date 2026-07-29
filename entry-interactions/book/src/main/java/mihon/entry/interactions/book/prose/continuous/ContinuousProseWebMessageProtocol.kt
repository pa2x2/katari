package mihon.entry.interactions.book.prose.continuous

import android.graphics.RectF
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import mihon.entry.interactions.book.document.model.BookDocumentBlockId
import mihon.entry.interactions.book.document.model.BookDocumentPosition

internal sealed interface ContinuousProseEvent {
    val generation: Long

    data class Ready(override val generation: Long) : ContinuousProseEvent

    data class Prepared(override val generation: Long) : ContinuousProseEvent

    data class Position(
        override val generation: Long,
        val sectionKey: String,
        val position: BookDocumentPosition,
        val progression: Float,
    ) : ContinuousProseEvent

    data class ChapterEntered(
        override val generation: Long,
        val sectionKey: String,
        val blockId: BookDocumentBlockId,
        val offsetWithinBlock: Int,
        val viewportOffset: Float,
    ) : ContinuousProseEvent

    data class TransitionReached(
        override val generation: Long,
        val destinationSectionKey: String,
    ) : ContinuousProseEvent

    data class TransitionRetry(
        override val generation: Long,
        val destinationSectionKey: String,
    ) : ContinuousProseEvent

    data class Tap(
        override val generation: Long,
        val fraction: Float,
    ) : ContinuousProseEvent

    data class ExternalLink(
        override val generation: Long,
        val url: String,
    ) : ContinuousProseEvent

    data class Selection(
        override val generation: Long,
        val identity: String,
        val text: String,
        val boundsInWebView: RectF,
    ) : ContinuousProseEvent

    data class SelectionCleared(override val generation: Long) : ContinuousProseEvent

    data class Failure(
        override val generation: Long,
        val reason: String,
    ) : ContinuousProseEvent
}

internal class ContinuousProseMessageValidator {
    fun parse(message: String, projection: ContinuousProseProjection): ContinuousProseEvent? {
        if (message.length !in 2..MAX_MESSAGE_CHARS) return null
        val root = runCatching { Json.parseToJsonElement(message).jsonObject }.getOrNull() ?: return null
        val generation = root.long("generation") ?: return null
        if (generation != projection.generation) return null
        return when (root.string("type")) {
            "ready" -> ContinuousProseEvent.Ready(generation)
            "prepared" -> ContinuousProseEvent.Prepared(generation)
            "position" -> parsePosition(root, generation, projection)
            "chapter-entered" -> parseChapterEntered(root, generation, projection)
            "transition-reached" -> root.string("destinationSectionKey")
                ?.takeIf(::isValidIdentifier)
                ?.let { ContinuousProseEvent.TransitionReached(generation, it) }
            "transition-retry" -> root.string("destinationSectionKey")
                ?.takeIf(::isValidIdentifier)
                ?.let { ContinuousProseEvent.TransitionRetry(generation, it) }
            "tap" -> root.finiteFloat("fraction")
                ?.takeIf { it in 0f..1f }
                ?.let { ContinuousProseEvent.Tap(generation, it) }
            "external-link" -> root.string("url")
                ?.takeIf { it.length <= MAX_URL_CHARS && isApprovedExternalUrl(it) }
                ?.let { ContinuousProseEvent.ExternalLink(generation, it) }
            "selection" -> parseSelection(root, generation)
            "selection-cleared" -> ContinuousProseEvent.SelectionCleared(generation)
            "failure" -> root.string("reason")
                ?.take(MAX_REASON_CHARS)
                ?.let { ContinuousProseEvent.Failure(generation, it) }
            else -> null
        }
    }

    private fun parsePosition(
        root: JsonObject,
        generation: Long,
        projection: ContinuousProseProjection,
    ): ContinuousProseEvent.Position? {
        val sectionKey = root.string("sectionKey")?.takeIf(::isValidIdentifier) ?: return null
        val blockId = root.string("blockId")?.takeIf(::isValidIdentifier) ?: return null
        val blockLength = projection.blockLengths[sectionKey]?.get(blockId) ?: return null
        val offset = root.int("offset")?.takeIf { it in 0..blockLength } ?: return null
        val progression = root.finiteFloat("progression")?.takeIf { it in 0f..1f } ?: return null
        return ContinuousProseEvent.Position(
            generation = generation,
            sectionKey = sectionKey,
            position = BookDocumentPosition(BookDocumentBlockId(blockId), offset),
            progression = progression,
        )
    }

    private fun parseChapterEntered(
        root: JsonObject,
        generation: Long,
        projection: ContinuousProseProjection,
    ): ContinuousProseEvent.ChapterEntered? {
        val sectionKey = root.string("sectionKey")?.takeIf(::isValidIdentifier) ?: return null
        val blockId = root.string("blockId")?.takeIf(::isValidIdentifier) ?: return null
        val blockLength = projection.blockLengths[sectionKey]?.get(blockId) ?: return null
        val offset = root.int("offset")?.takeIf { it in 0..blockLength } ?: return null
        val viewportOffset = root.finiteFloat("viewportOffset") ?: return null
        return ContinuousProseEvent.ChapterEntered(
            generation,
            sectionKey,
            BookDocumentBlockId(blockId),
            offset,
            viewportOffset,
        )
    }

    private fun parseSelection(root: JsonObject, generation: Long): ContinuousProseEvent.Selection? {
        val identity = root.string("identity")?.takeIf { it.length in 1..MAX_IDENTITY_CHARS } ?: return null
        val text = root.string("text")?.takeIf { it.length in 1..MAX_SELECTION_CHARS && it.isNotBlank() }
            ?: return null
        val left = root.finiteFloat("left") ?: return null
        val top = root.finiteFloat("top") ?: return null
        val right = root.finiteFloat("right") ?: return null
        val bottom = root.finiteFloat("bottom") ?: return null
        if (right <= left || bottom <= top) return null
        return ContinuousProseEvent.Selection(
            generation,
            identity,
            text,
            RectF(left, top, right, bottom),
        )
    }

    private fun isValidIdentifier(value: String): Boolean =
        value.length in 1..MAX_IDENTIFIER_CHARS && value.none(Char::isISOControl)

    private fun isApprovedExternalUrl(value: String): Boolean =
        value.startsWith("https://", ignoreCase = true) ||
            value.startsWith("http://", ignoreCase = true)

    private fun JsonObject.string(name: String): String? = get(name)?.jsonPrimitive?.contentOrNull

    private fun JsonObject.long(name: String): Long? = get(name)?.jsonPrimitive?.longOrNull

    private fun JsonObject.int(name: String): Int? = get(name)?.jsonPrimitive?.intOrNull

    private fun JsonObject.finiteFloat(name: String): Float? =
        get(name)?.jsonPrimitive?.floatOrNull?.takeIf(Float::isFinite)

    private companion object {
        const val MAX_MESSAGE_CHARS = 512 * 1024
        const val MAX_SELECTION_CHARS = 256 * 1024
        const val MAX_URL_CHARS = 8 * 1024
        const val MAX_IDENTIFIER_CHARS = 512
        const val MAX_IDENTITY_CHARS = 1024
        const val MAX_REASON_CHARS = 1024
    }
}

internal data class ContinuousProseRenderSettings(
    val backgroundCss: String,
    val foregroundCss: String,
    val fontFamilyCss: String,
    val fontSizePercent: Int,
    val lineHeightPercent: Int,
    val pageMarginsPercent: Int,
    val textAlignmentCss: String,
)

internal fun continuousProseRenderCommand(
    projection: ContinuousProseProjection,
    settings: ContinuousProseRenderSettings,
    initialSectionKey: String,
    initialPosition: BookDocumentPosition,
): String {
    val command = JsonObject(
        mapOf(
            "type" to kotlinx.serialization.json.JsonPrimitive("render"),
            "generation" to kotlinx.serialization.json.JsonPrimitive(projection.generation),
            "projection" to Json.parseToJsonElement(projection.json),
            "settings" to JsonObject(
                mapOf(
                    "background" to kotlinx.serialization.json.JsonPrimitive(settings.backgroundCss),
                    "foreground" to kotlinx.serialization.json.JsonPrimitive(settings.foregroundCss),
                    "fontFamily" to kotlinx.serialization.json.JsonPrimitive(settings.fontFamilyCss),
                    "fontSizePercent" to kotlinx.serialization.json.JsonPrimitive(settings.fontSizePercent),
                    "lineHeightPercent" to kotlinx.serialization.json.JsonPrimitive(settings.lineHeightPercent),
                    "pageMarginsPercent" to kotlinx.serialization.json.JsonPrimitive(settings.pageMarginsPercent),
                    "textAlignment" to kotlinx.serialization.json.JsonPrimitive(settings.textAlignmentCss),
                ),
            ),
            "initial" to JsonObject(
                mapOf(
                    "sectionKey" to kotlinx.serialization.json.JsonPrimitive(initialSectionKey),
                    "blockId" to kotlinx.serialization.json.JsonPrimitive(initialPosition.blockId.value),
                    "offset" to kotlinx.serialization.json.JsonPrimitive(initialPosition.offsetWithinBlock),
                ),
            ),
        ),
    )
    return command.toString()
}

internal fun continuousProseSeekCommand(
    generation: Long,
    sectionKey: String,
    position: BookDocumentPosition,
    smooth: Boolean,
): String = JsonObject(
    mapOf(
        "type" to kotlinx.serialization.json.JsonPrimitive("seek"),
        "generation" to kotlinx.serialization.json.JsonPrimitive(generation),
        "sectionKey" to kotlinx.serialization.json.JsonPrimitive(sectionKey),
        "blockId" to kotlinx.serialization.json.JsonPrimitive(position.blockId.value),
        "offset" to kotlinx.serialization.json.JsonPrimitive(position.offsetWithinBlock),
        "smooth" to kotlinx.serialization.json.JsonPrimitive(smooth),
    ),
).toString()

internal fun continuousProseClearSelectionCommand(generation: Long): String = JsonObject(
    mapOf(
        "type" to kotlinx.serialization.json.JsonPrimitive("clear-selection"),
        "generation" to kotlinx.serialization.json.JsonPrimitive(generation),
    ),
).toString()
