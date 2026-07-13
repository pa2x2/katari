package mihon.entry.interactions.book

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import mihon.book.api.BookLocator
import mihon.book.api.BookTextContext
import tachiyomi.domain.entry.model.EntryProgressLocator

internal object BookProgressLocatorCodec {
    private const val PRECISE_LOCATION_KEY = "app.katari.book.location"
    private const val RESOURCE_ID_KEY = "resourceId"
    private const val FRAGMENTS_KEY = "fragments"
    private const val TEXT_KEY = "text"
    private const val BEFORE_KEY = "before"
    private const val HIGHLIGHT_KEY = "highlight"
    private const val AFTER_KEY = "after"
    private const val PROCESSOR_EXTENSIONS_KEY = "processorExtensions"

    fun encode(
        locator: BookLocator,
        preservedExtensions: JsonObject = JsonObject(emptyMap()),
    ): EntryProgressLocator {
        val precise = buildMap<String, JsonElement> {
            put(RESOURCE_ID_KEY, JsonPrimitive(locator.resourceId))
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
            progression = locator.progression,
            totalProgression = locator.totalProgression,
            extensions = JsonObject(
                preservedExtensions + (PRECISE_LOCATION_KEY to JsonObject(precise)),
            ),
        )
    }

    fun decode(locator: EntryProgressLocator): BookLocator? {
        if (locator.kind != BOOK_PROGRESS_LOCATOR_KIND) return null
        val precise = locator.extensions[PRECISE_LOCATION_KEY] as? JsonObject ?: return null
        val resourceId = (precise[RESOURCE_ID_KEY] as? JsonPrimitive)
            ?.contentOrNull
            ?.takeIf(String::isNotBlank)
            ?: return null
        val fragments = (precise[FRAGMENTS_KEY] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
            .orEmpty()
        val text = (precise[TEXT_KEY] as? JsonObject)?.let { value ->
            BookTextContext(
                before = value.boundedText(BEFORE_KEY),
                highlight = value.boundedText(HIGHLIGHT_KEY),
                after = value.boundedText(AFTER_KEY),
            )
        }?.takeUnless { it == BookTextContext() }
        val processorExtensions = (precise[PROCESSOR_EXTENSIONS_KEY] as? JsonObject).orEmpty()
        val logicalPosition = locator.position
            ?.takeIf { it in 1..Int.MAX_VALUE.toLong() }
            ?.toInt()

        return BookLocator(
            resourceId = resourceId,
            progression = locator.progression,
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
