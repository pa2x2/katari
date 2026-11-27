package eu.kanade.tachiyomi.data.backup.conversion.aniyomi

import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Aniyomi's anime with seasons as Katari's entries merged by their source (D006). Aniyomi lists each season as an anime
 * of its own that names its parent's id; Katari makes the parent the merge's root, first, with its seasons after it in
 * the order the source lists them. Katari moves a root's listed members in and out of the library with it, so the
 * seasons take the parent's library state, and its next refresh locks the members the source still lists.
 */
internal class AniyomiSeasons(anime: List<AniyomiSeasonInfo>) {
    private val members: Map<Key, Member>

    /** The parents' titles, one row each in the report. */
    val parentTitles: List<String>

    /**
     * The titles of parents whose season list the person filtered, sorted or laid out their own way, which Katari has no
     * settings for (D058).
     */
    val changedViewTitles: List<String>

    init {
        val parents = anime.filter { it.fetchType == FETCH_SEASONS && it.id != null }
        val byId = parents.associateBy { it.id }
        val children = anime.filter { it.parentId != null && it.parentId in byId }.groupBy { it.parentId }
        members = buildMap {
            parents.forEach { parent ->
                val target = Key(parent.source, parent.url)
                put(target, Member(target, position = 0, parent.favorite, parent.categories, isRoot = true))
                children[parent.id].orEmpty().sortedBy { it.seasonSourceOrder }.forEachIndexed { index, season ->
                    put(
                        Key(season.source, season.url),
                        Member(target, position = index + 1, parent.favorite, parent.categories, isRoot = false),
                    )
                }
            }
        }
        parentTitles = parents.map { it.title }
        changedViewTitles = parents.filter { it.seasonFlags !in STOCK_SEASON_VIEWS }.map { it.title }
    }

    /**
     * The categories, as orders in the anime category list, an anime takes: its parent's for a season, its own for any
     * other.
     */
    fun categoriesOf(anime: AniyomiAnime): List<Long> = members[Key(anime.source, anime.url)]?.categories
        ?: anime.categories

    /** Makes [entry], converted from the anime at [sourceId] and [url], the root or a member of its merge. */
    fun applyTo(entry: BackupEntry, sourceId: Long, url: String, targetSourceKey: (Long) -> Long) {
        val member = members[Key(sourceId, url)] ?: return
        entry.mergedBySource = member.isRoot
        entry.mergeTargetSource = targetSourceKey(member.target.source)
        entry.mergeTargetUrl = member.target.url
        entry.mergeTargetType = EntryType.ANIME
        entry.mergePosition = member.position
        entry.favorite = member.favorite
    }

    private data class Key(val source: Long, val url: String)

    private class Member(
        val target: Key,
        val position: Int,
        val favorite: Boolean,
        val categories: List<Long>,
        val isRoot: Boolean,
    )

    private companion object {
        // Aniyomi's FetchType ordinals: Seasons, then Episodes
        const val FETCH_SEASONS = 0

        // A row Aniyomi never applied its defaults to, and Aniyomi's defaults: the unseen, local and continue overlays on
        // (tachiyomi.domain.library.service.LibraryPreferences, aniyomiorg/aniyomi 97414446b8a9)
        val STOCK_SEASON_VIEWS = setOf(0L, 0x05800000L)
    }
}

/** What an anime says about its seasons, read before any entry is written. */
@Serializable
internal class AniyomiSeasonInfo(
    @ProtoNumber(1) val source: Long,
    @ProtoNumber(2) val url: String,
    @ProtoNumber(3) val title: String = "",
    @ProtoNumber(17) val categories: List<Long> = emptyList(),
    @ProtoNumber(100) val favorite: Boolean = true,
    /** A season's parent's [id]. */
    @ProtoNumber(502) val parentId: Long? = null,
    /** The anime's row id in Aniyomi, written only to link seasons to their parent. */
    @ProtoNumber(503) val id: Long? = null,
    /** How a parent's season list is filtered, sorted and laid out. */
    @ProtoNumber(504) val seasonFlags: Long = 0,
    @ProtoNumber(506) val seasonSourceOrder: Long = 0,
    @ProtoNumber(507) val fetchType: Int = 1,
)
