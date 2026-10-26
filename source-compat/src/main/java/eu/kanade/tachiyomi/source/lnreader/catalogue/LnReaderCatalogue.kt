package eu.kanade.tachiyomi.source.lnreader.catalogue

import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.lnreader.runtime.LnReaderPluginCalls
import eu.kanade.tachiyomi.source.lnreader.runtime.asArray
import eu.kanade.tachiyomi.source.lnreader.runtime.asObject
import eu.kanade.tachiyomi.source.lnreader.runtime.text
import eu.kanade.tachiyomi.source.lnreader.web.LnReaderUrls
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * A plugin's novel lists. Like LNReader, a list ends on an empty page; Katari's pager also ends one on a page that
 * repeats what it already showed, as many plugins answer past the end.
 */
internal class LnReaderCatalogue(
    private val calls: LnReaderPluginCalls,
    private val urls: LnReaderUrls,
) {
    /** Popular, or with [latest] Latest, filtered by [filters]: the definitions with their values, as LNReader sends. */
    suspend fun popular(page: Int, latest: Boolean, filters: JsonObject?): EntryPageResult<SEntry> {
        val options = buildJsonObject {
            put("showLatestNovels", latest)
            filters?.let { put("filters", it) }
        }
        return novels(calls.call("popularNovels", JsonPrimitive(page), options))
    }

    suspend fun search(page: Int, query: String): EntryPageResult<SEntry> =
        novels(calls.call("searchNovels", JsonPrimitive(query), JsonPrimitive(page)))

    private fun novels(result: JsonElement): EntryPageResult<SEntry> {
        val novels = result.asArray().mapNotNull { it.asObject()?.toNovel() }
        return EntryPageResult(novels, hasNextPage = novels.isNotEmpty())
    }

    private fun JsonObject.toNovel(): SEntry? {
        val path = text("path")?.takeIf(String::isNotBlank) ?: return null
        return SEntry.create().also {
            it.url = path
            it.title = text("name")?.takeIf(String::isNotBlank) ?: path
            it.thumbnailUrl = urls.coverUrl(text("cover"))
            it.type = EntryType.BOOK
        }
    }
}
