package eu.kanade.tachiyomi.source.aniyomi.entry

import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.source.entry.ChapterKind
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter

/*
 * Aniyomi's models and the Entry SDK's carry the same fields under other names. An extension may return a title whose
 * lateinit url or title it never set; those are read with a check and fall back to what the host stored.
 */

internal fun AnimesPage.toEntryPageResult(): EntryPageResult<SEntry> =
    EntryPageResult(animes.map { it.toSEntry() }, hasNextPage)

/** [stored] is the entry the host asked about: its URL is the row's identity and always wins. */
internal fun SAnime.toSEntry(stored: SEntry? = null): SEntry = SEntry.create().also {
    val url = stored?.url ?: readInitialized { url }.orEmpty()
    it.url = url
    it.title = readInitialized { title } ?: stored?.title ?: url
    it.artist = artist
    it.author = author
    it.description = description
    it.genre = getGenres()
    it.status = status
    it.thumbnailUrl = thumbnail_url
    it.backgroundUrl = background_url
    it.updateStrategy = update_strategy.toEntryUpdateStrategy()
    it.initialized = initialized
    it.memo = memo
    it.type = EntryType.ANIME
    // Episodes is the default an extension's details parser may never have changed, so only Seasons is taken as said
    it.merged = fetch_type == FetchType.Seasons || stored?.merged == true
}

internal fun SEntry.toSAnime(): SAnime = SAnime.create().also {
    it.url = url
    it.title = title
    it.artist = artist
    it.author = author
    it.description = description
    it.genre = genre?.joinToString(", ")
    it.status = status
    it.thumbnail_url = thumbnailUrl
    it.background_url = backgroundUrl
    it.update_strategy = updateStrategy?.toAnimeUpdateStrategy() ?: AnimeUpdateStrategy.ALWAYS_UPDATE
    it.fetch_type = if (merged) FetchType.Seasons else FetchType.Episodes
    it.initialized = initialized
    it.memo = memo
}

internal fun SEpisode.toSEntryChapter(): SEntryChapter = SEntryChapter.create().also {
    it.url = url
    it.name = name
    it.dateUpload = date_upload
    it.chapterNumber = episode_number.toDouble()
    it.scanlator = scanlator
    it.summary = summary?.takeIf(String::isNotBlank)
    it.previewUrl = preview_url?.takeIf(String::isNotBlank)
    it.kind = if (fillermark) ChapterKind.FILLER else ChapterKind.REGULAR
    it.memo = memo
}

internal fun SEntryChapter.toSEpisode(): SEpisode = SEpisode.create().also {
    it.url = url
    it.name = name
    it.date_upload = dateUpload
    it.episode_number = chapterNumber.toFloat()
    it.scanlator = scanlator
    it.summary = summary
    it.preview_url = previewUrl
    it.fillermark = kind == ChapterKind.FILLER
    it.memo = memo
}

private fun AnimeUpdateStrategy.toEntryUpdateStrategy(): EntryUpdateStrategy = when (this) {
    AnimeUpdateStrategy.ALWAYS_UPDATE -> EntryUpdateStrategy.ALWAYS_UPDATE
    AnimeUpdateStrategy.ONLY_FETCH_ONCE -> EntryUpdateStrategy.ONLY_FETCH_ONCE
}

private fun EntryUpdateStrategy.toAnimeUpdateStrategy(): AnimeUpdateStrategy = when (this) {
    EntryUpdateStrategy.ALWAYS_UPDATE -> AnimeUpdateStrategy.ALWAYS_UPDATE
    EntryUpdateStrategy.ONLY_FETCH_ONCE -> AnimeUpdateStrategy.ONLY_FETCH_ONCE
}

private inline fun <T> readInitialized(block: () -> T): T? = try {
    block()
} catch (_: UninitializedPropertyAccessException) {
    null
}
