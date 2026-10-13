package app.katari.showcase.extension.media

import app.katari.showcase.extension.catalog.ShowcaseChapter
import app.katari.showcase.extension.catalog.ShowcaseEntryType
import app.katari.showcase.extension.catalog.ShowcaseWork
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookResourceAvailability

internal fun ShowcaseWork.media(chapter: ShowcaseChapter, baseUrl: String): EntryMedia = when (type) {
    ShowcaseEntryType.MANGA -> EntryMedia.ImagePages(
        chapter.files.mapIndexed { index, path -> EntryImagePage(index, imageUrl = "$baseUrl/$path") },
    )
    ShowcaseEntryType.BOOK -> epub("$baseUrl/${chapter.files.single()}")
    ShowcaseEntryType.ANIME -> video(chapter.files.single(), baseUrl)
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

private fun video(path: String, baseUrl: String): EntryMedia.Playback {
    val mimeType = when (path.substringAfterLast('.')) {
        "webm" -> "video/webm"
        "mp4" -> "video/mp4"
        else -> error("Unknown video container: $path")
    }
    val stream = VideoStream(
        request = VideoRequest("$baseUrl/$path"),
        label = "Original",
        type = VideoStreamType.PROGRESSIVE,
        mimeType = mimeType,
        key = "original",
    )
    return EntryMedia.Playback(PlaybackDescriptor(streams = listOf(stream)))
}
