package mihon.entry.interactions.book.state

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import mihon.book.api.BookLocator
import mihon.book.api.BookTextContext
import tachiyomi.domain.entry.model.EntryProgressLocator

internal object BookProgressLocatorCodec {
    private const val PRECISE_LOCATION_KEY = "app.katari.book.location"
    private const val RESOURCE_PROGRESSION_KEY = "resourceProgression"
    private const val RESOURCE_ID_KEY = "resourceId"
    private const val FRAGMENTS_KEY = "fragments"
    private const val TEXT_KEY = "text"
    private const val BEFORE_KEY = "before"
    private const val HIGHLIGHT_KEY = "highlight"
    private const val AFTER_KEY = "after"
    private const val PROCESSOR_EXTENSIONS_KEY = "processorExtensions"

    /** Chapter-facing columns use publication progress; the precision payload retains resource progress for resume. */
    fun encode(
        locator: BookLocator,
        preservedExtensions: JsonObject = JsonObject(emptyMap()),
        publicationProgression: Double? = null,
    ): EntryProgressLocator {
        val precise = buildMap<String, JsonElement> {
            put(RESOURCE_ID_KEY, JsonPrimitive(locator.resourceId))
            if (publicationProgression != null) {
                put(RESOURCE_PROGRESSION_KEY, locator.progression?.let(::JsonPrimitive) ?: JsonNull)
            }
            if (locator.fragments.isNotEmpty()) {
                put(FRAGMENTS_KEY, JsonArray(locator.fragments.map(::JsonPrimitive)))
            }
            locator.textContext?.let { text ->
                put(
                    TEXT_KEY,
                    JsonObject(
                        buildMap {
                            text.before?.let { put(BEFORE_KEY, JsonPrimitive(it)) }
                            text.highlight?.let { put(HIGHLIGHT_KEY, JsonPrimitive(it)) }
                            text.after?.let { put(AFTER_KEY, JsonPrimitive(it)) }
                        },
                    ),
                )
            }
            if (locator.extensions.isNotEmpty()) {
                put(PROCESSOR_EXTENSIONS_KEY, JsonObject(locator.extensions))
            }
        }
        return EntryProgressLocator(
            kind = BOOK_PROGRESS_LOCATOR_KIND,
            position = locator.logicalPosition?.toLong(),
            progression = publicationProgression ?: locator.progression,
            totalProgression = locator.totalProgression,
            extensions = JsonObject(
                preservedExtensions + (PRECISE_LOCATION_KEY to JsonObject(precise)),
            ),
        )
    }

    fun decode(
        locator: EntryProgressLocator,
        fallbackResourceId: String? = null,
    ): BookLocator? {
        if (locator.kind != BOOK_PROGRESS_LOCATOR_KIND) return null
        val precise = locator.extensions[PRECISE_LOCATION_KEY] as? JsonObject
        val preciseResourceId = (precise?.get(RESOURCE_ID_KEY) as? JsonPrimitive)
            ?.contentOrNull
            ?.takeIf(String::isNotBlank)
        val hasScalarLocation = locator.position != null ||
            locator.progression != null ||
            locator.totalProgression != null
        val resourceId = preciseResourceId
            ?: fallbackResourceId
                ?.takeIf(String::isNotBlank)
                ?.takeIf { precise == null && hasScalarLocation }
            ?: return null
        val fragments = (precise?.get(FRAGMENTS_KEY) as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
            .orEmpty()
        val text = (precise?.get(TEXT_KEY) as? JsonObject)?.let { value ->
            BookTextContext(
                before = value.boundedText(BEFORE_KEY),
                highlight = value.boundedText(HIGHLIGHT_KEY),
                after = value.boundedText(AFTER_KEY),
            )
        }?.takeUnless { it == BookTextContext() }
        val processorExtensions = (precise?.get(PROCESSOR_EXTENSIONS_KEY) as? JsonObject).orEmpty()
        val logicalPosition = locator.position
            ?.takeIf { it in 1..Int.MAX_VALUE.toLong() }
            ?.toInt()

        return BookLocator(
            resourceId = resourceId,
            progression = if (RESOURCE_PROGRESSION_KEY in precise.orEmpty()) {
                (precise?.get(RESOURCE_PROGRESSION_KEY) as? JsonPrimitive)?.doubleOrNull
            } else {
                locator.progression
            },
            totalProgression = locator.totalProgression,
            logicalPosition = logicalPosition,
            fragments = fragments,
            textContext = text,
            extensions = processorExtensions,
        )
    }

    private fun JsonObject.boundedText(key: String): String? {
        return (get(key) as? JsonPrimitive)?.contentOrNull?.take(BookTextContext.MAX_LENGTH)
    }
}
