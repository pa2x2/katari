package mihon.entry.interactions.book.epub

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.book.api.BookPublication
import mihon.book.api.BookReadingDirection
import mihon.book.api.BookResource
import mihon.book.api.BookTextContext
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

internal object ReadiumPublicationAdapter {

    fun adapt(
        publication: Publication,
        publicationId: String,
        revision: String,
    ): BookPublication = BookPublication(
        id = publicationId,
        revision = revision,
        title = publication.metadata.title,
        languages = publication.metadata.languages,
        readingDirection = when (publication.metadata.readingProgression) {
            ReadingProgression.LTR -> BookReadingDirection.LEFT_TO_RIGHT
            ReadingProgression.RTL -> BookReadingDirection.RIGHT_TO_LEFT
            null -> null
        },
        readingOrder = publication.readingOrder.map(::adaptResource),
        navigation = publication.tableOfContents.map(::adaptNavigation),
    )

    private fun adaptResource(link: Link) = BookResource(
        id = link.href.toString(),
        mediaType = link.mediaType?.toString(),
        title = link.title,
    )

    private fun adaptNavigation(link: Link): BookNavigationItem = BookNavigationItem(
        title = link.title,
        target = BookLocator(
            resourceId = link.url().removeFragment().toString(),
            fragments = listOfNotNull(link.url().fragment),
        ),
        children = link.children.map(::adaptNavigation),
    )
}

internal object ReadiumLocatorAdapter {
    private const val READIUM_LOCATIONS_EXTENSION = "org.readium.locations"

    fun adapt(locator: Locator): BookLocator = BookLocator(
        resourceId = locator.href.removeFragment().toString(),
        progression = locator.locations.progression,
        totalProgression = locator.locations.totalProgression,
        logicalPosition = locator.locations.position,
        fragments = locator.locations.fragments.ifEmpty { listOfNotNull(locator.href.fragment) },
        textContext = BookTextContext(
            before = locator.text.before.bounded(),
            highlight = locator.text.highlight.bounded(),
            after = locator.text.after.bounded(),
        ).takeUnless { it == BookTextContext() },
        extensions = locator.locations.otherLocations
            .takeIf { it.isNotEmpty() }
            ?.let { mapOf(READIUM_LOCATIONS_EXTENSION to it.toJsonElement()) }
            .orEmpty(),
    )

    fun restore(locator: BookLocator, publication: Publication): Locator? {
        val resourceHref = Url(locator.resourceId) ?: return null
        val link = publication.linkWithHref(resourceHref) ?: return null
        val readiumLocations = (locator.extensions[READIUM_LOCATIONS_EXTENSION] as? JsonObject)
            ?.mapValues { it.value.toReadiumValue() }
            .orEmpty()
        return Locator(
            href = resourceHref,
            mediaType = link.mediaType ?: MediaType.XHTML,
            title = link.title,
            locations = Locator.Locations(
                fragments = locator.fragments,
                progression = locator.progression,
                position = locator.logicalPosition,
                totalProgression = locator.totalProgression,
                otherLocations = readiumLocations,
            ),
            text = Locator.Text(
                before = locator.textContext?.before,
                highlight = locator.textContext?.highlight,
                after = locator.textContext?.after,
            ),
        )
    }

    private fun String?.bounded(): String? = this?.take(BookTextContext.MAX_LENGTH)

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Map<*, *> -> JsonObject(entries.associate { (key, value) -> key.toString() to value.toJsonElement() })
        is Iterable<*> -> JsonArray(map { it.toJsonElement() })
        else -> JsonPrimitive(toString())
    }

    private fun JsonElement.toReadiumValue(): Any = when (this) {
        JsonNull -> ""
        is JsonArray -> map { it.toReadiumValue() }
        is JsonObject -> mapValues { it.value.toReadiumValue() }
        is JsonPrimitive -> booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
    }
}
