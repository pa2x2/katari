package eu.kanade.tachiyomi.data.backup.conversion.mihon

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.stream.decodeBackup
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.i18n.*

/**
 * TachiyomiSY's, Komikku's and Anikku's merged entries, read before the entries are converted so each member is written
 * with its merge (D013, D070). A merged entry is a stand-in of the merged source listing its members; in Katari the
 * members themselves form the merge, with the one whose details it shows as its target. Keys are the backup's own
 * source ids.
 */
internal class MihonMerges private constructor(
    private val members: Map<EntryKey, Member>,
    private val merges: List<Merge>,
    private val type: EntryType,
) {
    private class Merge(
        val title: String,
        /** Members of SY's built-in sources, which stay outside the Katari merge (D047). */
        val builtInSourceMembers: Int,
        val hasChapterSortMode: Boolean,
        /** Members whose new chapters SY didn't download; Katari has no such switch per member (D038). */
        val downloadsOff: Int,
    )

    private class Member(
        val target: EntryKey,
        val position: Int,
        val updatesOff: Boolean,
        val favorite: Boolean,
        val categories: List<Long>,
        val displayName: String?,
    )

    /** The merges as the report's merged entries row lists them. */
    val reportItems: List<Item>
        get() = merges.map { Item(it.title) }

    /** What of the merges stays behind, for the merged entries row. */
    val leftBehind: List<Item>
        get() = merges.flatMap { merge ->
            buildList {
                repeat(merge.builtInSourceMembers) {
                    add(Item(merge.title, MR.strings.backup_conversion_merge_builtin_source))
                }
                if (merge.hasChapterSortMode) add(Item(merge.title, MR.strings.backup_conversion_merge_sort_mode))
                repeat(merge.downloadsOff) {
                    add(Item(merge.title, MR.strings.backup_conversion_merge_download_switch))
                }
            }
        }

    fun isMergedEntry(source: Long): Boolean = source == MERGED_SOURCE_ID

    fun isMember(source: Long, url: String): Boolean = EntryKey(source, url) in members

    /**
     * Makes [entry], the backup's entry from [source], a member of its merge, which holds the library state SY kept on
     * the merged entry. [sourceKey] gives the key the converted backup lists a source under.
     */
    fun applyTo(entry: BackupEntry, source: Long = entry.source, sourceKey: (Long) -> Long = { it }) {
        val member = members[EntryKey(source, entry.url)] ?: return
        entry.mergeTargetSource = sourceKey(member.target.source)
        entry.mergeTargetUrl = member.target.url
        entry.mergeTargetType = type
        entry.mergePosition = member.position
        entry.favorite = member.favorite
        entry.categories = member.categories
        member.displayName?.let { entry.displayName = it }
        // D037
        if (member.updatesOff) entry.updateMode = EntryUpdateMode.NEVER.name.lowercase()
    }

    /**
     * Collects the merges among the entries of [type] from their encoded entries, one at a time and in any order;
     * [build] gives the merges once every entry has been added.
     */
    class Collector(private val parser: ProtoBuf, private val type: EntryType = EntryType.MANGA) {
        private val members = mutableMapOf<EntryKey, Member>()
        private val merges = mutableListOf<Merge>()
        private val seen = hashSetOf<String>()

        fun add(entry: ByteArray) {
            val merged = parser.decodeBackup(MergedEntry.serializer(), entry)
            // TachiyomiSY writes each merged entry twice
            if (merged.source != MERGED_SOURCE_ID || !seen.add(merged.url)) return
            merges += merged.plan(members)
        }

        fun build() = MihonMerges(members, merges, type)
    }

    companion object {
        /** TachiyomiSY's merged source (exh.source.SourceIds.MERGED_SOURCE_ID), which Komikku keeps. */
        const val MERGED_SOURCE_ID = 6969L

        /** TachiyomiSY's built-in E-Hentai and ExHentai. */
        private val BUILT_IN_SOURCE_IDS = setOf(6901L, 6902L)

        private fun MergedEntry.plan(members: MutableMap<EntryKey, Member>): Merge {
            val (builtIn, titles) = references
                .filter { it.mangaSourceId != MERGED_SOURCE_ID }
                .partition { it.mangaSourceId in BUILT_IN_SOURCE_IDS }
            val ordered = titles.sortedBy { it.chapterPriority }
            val info = ordered.firstOrNull { it.isInfoManga }
                ?: settings?.let { named ->
                    ordered.firstOrNull {
                        it.mangaUrl == named.infoMangaUrl && it.mangaSourceId == named.infoMangaSourceId
                    }
                }
                ?: ordered.firstOrNull()
            if (info != null) {
                val target = EntryKey(info.mangaSourceId, info.mangaUrl)
                ordered.forEachIndexed { position, reference ->
                    val key = EntryKey(reference.mangaSourceId, reference.mangaUrl)
                    members[key] = Member(
                        target = target,
                        position = position,
                        updatesOff = !reference.fetchChapterUpdates,
                        favorite = favorite,
                        categories = categories,
                        displayName = customTitle.takeIf { key == target },
                    )
                }
            }
            return Merge(
                title = customTitle ?: title,
                builtInSourceMembers = builtIn.size,
                hasChapterSortMode = settings != null || references.any { it.chapterSortMode != 0 },
                downloadsOff = titles.count { !it.downloadChapters },
            )
        }
    }
}

internal data class EntryKey(val source: Long, val url: String)

/** A Mihon-family or Anikku entry read only for what a merged entry holds, which skips its chapters. */
@Serializable
private class MergedEntry(
    @ProtoNumber(1) val source: Long,
    @ProtoNumber(2) val url: String,
    @ProtoNumber(3) val title: String = "",
    @ProtoNumber(17) val categories: List<Long> = emptyList(),
    @ProtoNumber(100) val favorite: Boolean = true,
    @ProtoNumber(600) val references: List<MihonMergeReference> = emptyList(),
    @ProtoNumber(604) val settings: MihonMergeSettings? = null,
    @ProtoNumber(800) val customTitle: String? = null,
)
