package app.katari.showcase.extension.media

import app.katari.showcase.extension.catalog.ShowcaseChapter
import app.katari.showcase.extension.catalog.ShowcaseEntryType
import app.katari.showcase.extension.catalog.ShowcaseVideoVersion
import app.katari.showcase.extension.catalog.ShowcaseWork
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookResourceAvailability

internal fun ShowcaseWork.media(chapter: ShowcaseChapter, selection: PlaybackSelection, baseUrl: String): EntryMedia =
    when (type) {
        ShowcaseEntryType.MANGA -> EntryMedia.ImagePages(
            chapter.files.mapIndexed { index, path -> EntryImagePage(index, imageUrl = "$baseUrl/$path") },
        )
        ShowcaseEntryType.BOOK -> epub("$baseUrl/${chapter.files.single()}")
        ShowcaseEntryType.ANIME -> playback(chapter.versions, selection, baseUrl)
    }

private fun ShowcaseWork.epub(url: String): EntryMedia.Book {
    val resourceId = "$id-publication"
    val location = BookResourceLocation.RemoteRequest(url)
    return EntryMedia.Book(
        descriptor = BookContentDescriptor("application/epub+zip"),
        catalog = BookResourceCatalog(
            resources = listOf(
                BookSourceResource(
                    id = resourceId,
                    title = title,
                    order = 0,
                    mediaType = "application/epub+zip",
                    location = location,
                    availability = BookResourceAvailability.AVAILABLE,
                ),
            ),
            coverage = BookCatalogCoverage.COMPLETE,
        ),
        initialResourceId = resourceId,
        initialResourceLocation = location,
    )
}

/**
 * The episode's versions as the player's settings offer them: every server, the qualities of the selected one, and the
 * selected version as the only stream. A selection that names no version picks the first, the one the scenes play.
 */
private fun playback(
    versions: List<ShowcaseVideoVersion>,
    selection: PlaybackSelection,
    baseUrl: String,
): EntryMedia.Playback {
    val servers = versions.map { it.server }.distinct()
    val server = selection.serverKey?.takeIf { it in servers } ?: servers.first()
    val serverVersions = versions.filter { it.server == server }
    val version = serverVersions.firstOrNull { it.quality == selection.sourceQualityKey } ?: serverVersions.first()
    val stream = VideoStream(
        request = VideoRequest("$baseUrl/${version.file}"),
        label = version.quality,
        type = VideoStreamType.PROGRESSIVE,
        mimeType = mimeType(version.file),
        key = version.quality,
    )
    return EntryMedia.Playback(
        PlaybackDescriptor(
            selection = PlaybackSelection(serverKey = server, sourceQualityKey = version.quality),
            servers = servers.map { VideoPlaybackOption(key = it, label = it) },
            sourceQualities = serverVersions.map { VideoPlaybackOption(key = it.quality, label = it.quality) },
            streams = listOf(stream),
        ),
    )
}

private fun mimeType(path: String): String = when (path.substringAfterLast('.')) {
    "webm" -> "video/webm"
    "mp4" -> "video/mp4"
    else -> error("Unknown video container: $path")
}
