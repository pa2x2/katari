package app.katari.showcase.extension

import app.katari.showcase.extension.catalog.ShowcaseCatalogClient
import app.katari.showcase.extension.catalog.ShowcaseSourceDefinition
import app.katari.showcase.extension.catalog.ShowcaseWork
import app.katari.showcase.extension.media.media
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryImageHttpSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SourceMetadata
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * One source of the showcase catalog. Every type extends the image source base because only the manga reader uses it;
 * entries are addressed as `/<work id>` and chapters as `/<work id>/<chapter number>`.
 */
internal class ShowcaseSource(
    private val definition: ShowcaseSourceDefinition,
    private val catalog: ShowcaseCatalogClient,
) : EntryImageHttpSource(), SourceMetadata {

    override val name: String = definition.name
    override val lang: String = definition.language
    override val baseUrl: String = BuildConfig.CONTENT_SERVER
    override val supportsLatest: Boolean = true
    override val supportedEntryTypes: Set<EntryType> = setOf(definition.type.entryType)

    override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> =
        EntryPageResult(works().map { it.toEntry() }, hasNextPage = false)

    override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> =
        EntryPageResult(
            works().sortedByDescending { work -> work.chapters.maxOf { it.date } }.map { it.toEntry() },
            hasNextPage = false,
        )

    override suspend fun getSearchContent(page: Int, query: String, filters: EntryFilterList): EntryPageResult<SEntry> =
        EntryPageResult(
            works()
                .filter { work ->
                    (listOf(work.title, work.author) + work.genres).any { it.contains(query, ignoreCase = true) }
                }
                .map { it.toEntry() },
            hasNextPage = false,
        )

    override suspend fun getContentDetails(entry: SEntry): SEntry = work(entry.url).toEntry()

    override suspend fun getChapterList(entry: SEntry): List<SEntryChapter> {
        val work = work(entry.url)
        return work.chapters.sortedByDescending { it.number }.map { chapter ->
            SEntryChapter.create().apply {
                url = "/${work.id}/${chapter.number}"
                name = chapter.name
                chapterNumber = chapter.number.toDouble()
                dateUpload = LocalDate.parse(chapter.date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }
        }
    }

    override suspend fun getMedia(chapter: SEntryChapter, selection: PlaybackSelection): EntryMedia {
        val (workId, number) = chapter.url.removePrefix("/").split('/')
        val work = work(workId)
        return work.media(work.chapters.single { it.number == number.toInt() }, baseUrl)
    }

    private suspend fun works(): List<ShowcaseWork> =
        catalog.catalog(client, headers).works.filter { it.source == definition.id }

    private suspend fun work(url: String): ShowcaseWork {
        val id = url.removePrefix("/")
        return works().singleOrNull { it.id == id } ?: error("The showcase catalog has no entry $id in $name")
    }

    private fun ShowcaseWork.toEntry(): SEntry = SEntry.create().also { entry ->
        entry.url = "/$id"
        entry.title = title
        entry.author = author
        entry.artist = artist
        entry.description = description
        entry.genre = genres
        entry.status = when (status) {
            "ongoing" -> SEntry.ONGOING
            "completed" -> SEntry.COMPLETED
            else -> SEntry.UNKNOWN
        }
        entry.thumbnailUrl = "$baseUrl/$cover"
        entry.type = type.entryType
        entry.initialized = true
    }
}
