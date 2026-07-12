package eu.kanade.domain.source.model

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
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
import tachiyomi.domain.source.interactor.GetRemoteCatalog

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

@Serializable
sealed interface FilterStateNode {
    val name: String

    @Serializable
    @SerialName("header")
    data class Header(
        override val name: String,
    ) : FilterStateNode

    @Serializable
    @SerialName("separator")
    data class Separator(
        override val name: String,
    ) : FilterStateNode

    @Serializable
    @SerialName("select")
    data class Select(
        override val name: String,
        val state: Int,
    ) : FilterStateNode

    @Serializable
    @SerialName("text")
    data class Text(
        override val name: String,
        val state: String,
    ) : FilterStateNode

    @Serializable
    @SerialName("checkbox")
    data class CheckBox(
        override val name: String,
        val state: Boolean,
    ) : FilterStateNode

    @Serializable
    @SerialName("tristate")
    data class TriState(
        override val name: String,
        val state: Int,
    ) : FilterStateNode

    @Serializable
    @SerialName("sort")
    data class Sort(
        override val name: String,
        val index: Int?,
        val ascending: Boolean?,
    ) : FilterStateNode

    @Serializable
    @SerialName("group")
    data class Group(
        override val name: String,
        val state: List<FilterStateNode>,
    ) : FilterStateNode
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
            FeedListingMode.Popular -> GetRemoteCatalog.QUERY_POPULAR
            FeedListingMode.Latest -> GetRemoteCatalog.QUERY_LATEST
            FeedListingMode.Search -> query
        }
}

fun EntryFilterList.snapshot(): List<FilterStateNode> {
    return map(EntryFilter<*>::toNode)
}

fun EntryFilterList.applySnapshot(snapshot: List<FilterStateNode>): EntryFilterList {
    applyNodes(filters = this, nodes = snapshot)
    return this
}

private fun applyNodes(filters: List<EntryFilter<*>>, nodes: List<FilterStateNode>) {
    filters.zip(nodes).forEach { (filter, node) ->
        when {
            filter.name != node.name -> return@forEach
            filter is EntryFilter.Select<*> && node is FilterStateNode.Select -> {
                filter.state = node.state.coerceIn(0, filter.values.lastIndex)
            }
            filter is EntryFilter.Text && node is FilterStateNode.Text -> {
                filter.state = node.state
            }
            filter is EntryFilter.CheckBox && node is FilterStateNode.CheckBox -> {
                filter.state = node.state
            }
            filter is EntryFilter.TriState && node is FilterStateNode.TriState -> {
                filter.state = node.state
            }
            filter is EntryFilter.Sort && node is FilterStateNode.Sort -> {
                filter.state = if (node.index == null || node.ascending == null) {
                    null
                } else {
                    EntryFilter.Sort.Selection(
                        index = node.index.coerceIn(0, filter.values.lastIndex),
                        ascending = node.ascending,
                    )
                }
            }
            filter is EntryFilter.Group<*> && node is FilterStateNode.Group -> {
                applyNodes(filter.state.filterIsInstance<EntryFilter<*>>(), node.state)
            }
        }
    }
}

private fun EntryFilter<*>.toNode(): FilterStateNode {
    return when (this) {
        is EntryFilter.Header -> FilterStateNode.Header(name)
        is EntryFilter.Separator -> FilterStateNode.Separator(name)
        is EntryFilter.Select<*> -> FilterStateNode.Select(name, state)
        is EntryFilter.Text -> FilterStateNode.Text(name, state)
        is EntryFilter.CheckBox -> FilterStateNode.CheckBox(name, state)
        is EntryFilter.TriState -> FilterStateNode.TriState(name, state)
        is EntryFilter.Sort -> FilterStateNode.Sort(name, state?.index, state?.ascending)
        is EntryFilter.Group<*> -> FilterStateNode.Group(
            name = name,
            state = state.filterIsInstance<EntryFilter<*>>().map(EntryFilter<*>::toNode),
        )
    }
}
