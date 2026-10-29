package eu.kanade.tachiyomi.source.lnreader.entry

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.lnreader.runtime.asArray
import eu.kanade.tachiyomi.source.lnreader.runtime.asNumber
import eu.kanade.tachiyomi.source.lnreader.runtime.text
import eu.kanade.tachiyomi.source.lnreader.web.LnReaderUrls
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** What `parseNovel` gives: the novel's details, its chapters or page 1's, and how many pages its list has. */
internal class LnReaderNovel(private val fields: JsonObject) {
    val chapters: JsonArray
        get() = fields["chapters"].asArray()

    /** 0 for a list that isn't paged. */
    val totalPages: Int
        get() = fields["totalPages"].asNumber()?.toInt()?.coerceAtLeast(0) ?: 0

    /** [entry] with the novel's details; what the plugin leaves out stays as it was. */
    fun details(entry: SEntry, urls: LnReaderUrls): SEntry = entry.copy().also {
        fields.text("name")?.takeIf(String::isNotBlank)?.let { name -> it.title = name.trim() }
        it.author = fields.text("author")?.trim()?.takeIf(String::isNotEmpty)
        it.artist = fields.text("artist")?.trim()?.takeIf(String::isNotEmpty)
        it.description = fields.text("summary")?.trim()?.takeIf(String::isNotEmpty)
        it.genre = fields.text("genres")
            ?.split(',')
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.distinct()
            ?.takeIf(List<String>::isNotEmpty)
        it.status = lnReaderStatus(fields.text("status"))
        urls.coverUrl(fields.text("cover"))?.let { cover -> it.thumbnailUrl = cover }
        it.type = EntryType.BOOK
    }
}
