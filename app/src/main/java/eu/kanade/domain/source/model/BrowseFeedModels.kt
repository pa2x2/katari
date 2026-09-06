package eu.kanade.domain.source.model

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import tachiyomi.domain.library.model.LibraryDisplayMode

const val BUILTIN_POPULAR_PRESET_ID = "builtin:popular"
const val BUILTIN_LATEST_PRESET_ID = "builtin:latest"

@Serializable(with = FeedItemRef.Serializer::class)
data class FeedItemRef(
    val id: Long,
    val type: EntryType,
) {

    object Serializer : KSerializer<FeedItemRef> {
        override val descriptor = buildClassSerialDescriptor("FeedItemRef") {
            element<Long>("id")
            element<String>("type")
        }

        override fun serialize(encoder: Encoder, value: FeedItemRef) {
            val jsonEncoder = encoder as? JsonEncoder
                ?: throw SerializationException("FeedItemRef can only be serialized as JSON")
            jsonEncoder.encodeJsonElement(
                buildJsonObject {
                    put("id", value.id)
                    put("type", value.type.name)
                },
            )
        }

        override fun deserialize(decoder: Decoder): FeedItemRef {
            val jsonDecoder = decoder as? JsonDecoder
                ?: throw SerializationException("FeedItemRef can only be deserialized from JSON")
            val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
            val id = jsonObject["id"]?.jsonPrimitive?.longOrNull
                ?: throw SerializationException("FeedItemRef.id is missing")
            val type = jsonObject["type"]?.jsonPrimitive?.contentOrNull
                ?.toEntryType()
                ?: throw SerializationException("FeedItemRef.type is missing")
            return FeedItemRef(id = id, type = type)
        }

        private fun String.toEntryType(): EntryType {
            return when (this) {
                EntryType.MANGA.name, "manga" -> EntryType.MANGA
                EntryType.ANIME.name, "anime" -> EntryType.ANIME
                EntryType.BOOK.name, "book" -> EntryType.BOOK
                else -> throw SerializationException("Unknown FeedItemRef.type: $this")
            }
        }
    }
}

@Serializable
enum class SourceFeedContentMode {
    @SerialName("browse")
    Browse,

    @SerialName("video")
    Video,
}

@Serializable
data class SourceFeedPreset(
    val id: String,
    val sourceId: Long,
    val name: String,
    val listingMode: FeedListingMode,
    val chronological: Boolean = true,
    val query: String? = null,
    val filters: List<FilterStateNode> = emptyList(),
)

@Serializable
data class SourceFeed(
    val id: String,
    val contentMode: SourceFeedContentMode = SourceFeedContentMode.Browse,
    val sourceId: Long,
    val presetId: String,
    val enabled: Boolean = true,
    val displayMode: String? = null,
)

fun SourceFeed.resolvedDisplayMode(defaultDisplayMode: LibraryDisplayMode): LibraryDisplayMode {
    return displayMode?.let(LibraryDisplayMode::deserialize) ?: defaultDisplayMode
}

fun popularFeedPreset(sourceId: Long, name: String): SourceFeedPreset {
    return SourceFeedPreset(
        id = BUILTIN_POPULAR_PRESET_ID,
        sourceId = sourceId,
        name = name,
        listingMode = FeedListingMode.Popular,
        chronological = false,
    )
}

fun latestFeedPreset(sourceId: Long, name: String): SourceFeedPreset {
    return SourceFeedPreset(
        id = BUILTIN_LATEST_PRESET_ID,
        sourceId = sourceId,
        name = name,
        listingMode = FeedListingMode.Latest,
        chronological = true,
    )
}

@Serializable
data class SourceFeedTimeline(
    val items: List<FeedItemRef> = emptyList(),
    @SerialName("mangaIds")
    val legacyMangaIds: List<Long> = emptyList(),
    val nextPageKey: Long? = null,
) {
    fun resolvedItems(): List<FeedItemRef> {
        return items.takeIf { it.isNotEmpty() }
            ?: legacyMangaIds.map { FeedItemRef(it, EntryType.MANGA) }
    }

    companion object {
        fun fromItems(items: List<FeedItemRef>, nextPageKey: Long?): SourceFeedTimeline {
            return SourceFeedTimeline(
                items = items,
                legacyMangaIds = emptyList(),
                nextPageKey = nextPageKey,
            )
        }
    }
}

@Serializable
data class SourceFeedAnchor(
    val item: FeedItemRef? = null,
    @SerialName("mangaId")
    val legacyMangaId: Long? = null,
    val scrollOffset: Int = 0,
) {
    fun resolvedItem(): FeedItemRef? {
        return item ?: legacyMangaId?.let { FeedItemRef(it, EntryType.MANGA) }
    }

    companion object {
        fun fromItem(item: FeedItemRef?, scrollOffset: Int): SourceFeedAnchor {
            return SourceFeedAnchor(
                item = item,
                legacyMangaId = null,
                scrollOffset = scrollOffset,
            )
        }
    }
}

@Serializable
enum class FeedListingMode {
    @SerialName("popular")
    Popular,

    @SerialName("latest")
    Latest,

    @SerialName("search")
    Search,
}

fun SourceFeedPreset.toListing(): FeedSavedListing {
    return FeedSavedListing(
        mode = listingMode,
        query = query,
        filters = filters,
    )
}

data class FeedSavedListing(
    val mode: FeedListingMode,
    val query: String? = null,
    val filters: List<FilterStateNode> = emptyList(),
) {
    val requestQuery: String?
        get() = when (mode) {
            FeedListingMode.Popular -> CATALOGUE_POPULAR_QUERY
            FeedListingMode.Latest -> CATALOGUE_LATEST_QUERY
            FeedListingMode.Search -> query
        }
}
