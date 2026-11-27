package eu.kanade.tachiyomi.data.backup.conversion.mihon

import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.stream.ProtoInput
import eu.kanade.tachiyomi.data.backup.stream.decodeBackup
import eu.kanade.tachiyomi.data.backup.stream.lengthDelimitedTag
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import okio.BufferedSource
import tachiyomi.domain.library.update.model.EntryUpdateMode

/**
 * TachiyomiSY's and Komikku's merged entries, read before the entries are converted so each member is written with
 * its merge (D013). A merged entry is a stand-in of the merged source listing its members; in Katari the members
 * themselves form the merge, with the one whose details it shows as its target.
 */
internal class MihonMerges private constructor(
    private val members: Map<EntryKey, Member>,
    val merges: List<Merge>,
) {
    class Merge(
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

    fun isMergedEntry(manga: MihonManga): Boolean = manga.source == MERGED_SOURCE_ID

    fun isMember(manga: MihonManga): Boolean = EntryKey(manga.source, manga.url) in members

    /** Makes [entry] a member of its merge, which holds the library state SY kept on the merged entry. */
    fun applyTo(entry: BackupEntry) {
        val member = members[EntryKey(entry.source, entry.url)] ?: return
        entry.mergeTargetSource = member.target.source
        entry.mergeTargetUrl = member.target.url
        entry.mergeTargetType = EntryType.MANGA
        entry.mergePosition = member.position
        entry.favorite = member.favorite
        entry.categories = member.categories
        member.displayName?.let { entry.displayName = it }
        // D037
        if (member.updatesOff) entry.updateMode = EntryUpdateMode.NEVER.name.lowercase()
    }

    companion object {
        /** TachiyomiSY's merged source (exh.source.SourceIds.MERGED_SOURCE_ID), which Komikku keeps. */
        const val MERGED_SOURCE_ID = 6969L

        /** TachiyomiSY's built-in E-Hentai and ExHentai. */
        private val BUILT_IN_SOURCE_IDS = setOf(6901L, 6902L)

        private val ENTRY_TAG = lengthDelimitedTag(1)

        fun read(content: BufferedSource, parser: ProtoBuf): MihonMerges {
            val members = mutableMapOf<EntryKey, Member>()
            val merges = mutableListOf<Merge>()
            val seen = hashSetOf<String>()
            val input = ProtoInput(content)
            while (!input.exhausted()) {
                val tag = input.readTag()
                if (tag != ENTRY_TAG) {
                    input.skipField(tag)
                    continue
                }
                val entry = parser.decodeBackup(MergedEntry.serializer(), input.readByteArray(input.readLength()))
                // TachiyomiSY writes each merged entry twice
                if (entry.source != MERGED_SOURCE_ID || !seen.add(entry.url)) continue
                merges += entry.plan(members)
            }
            return MihonMerges(members, merges)
        }

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

/** A Mihon-family entry read only for what a merged entry holds, which skips its chapters. */
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
