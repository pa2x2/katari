package tachiyomi.source.local.metadata

import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.core.metadata.comicinfo.uploadDateMillis
import tachiyomi.source.local.LocalEntryChapterMetadata
import tachiyomi.source.local.LocalEntryMetadata

internal fun LocalEntryMetadata.getComicInfo() = ComicInfo(
    series = ComicInfo.Series(title),
    summary = description?.let { ComicInfo.Summary(it) },
    writer = author?.let { ComicInfo.Writer(it) },
    penciller = artist?.let { ComicInfo.Penciller(it) },
    genre = genre?.joinToString(", ")?.let { ComicInfo.Genre(it) },
    publishingStatus = ComicInfo.PublishingStatusTachiyomi(toComicInfoPublishingStatus(status)),
    title = null,
    number = null,
    web = null,
    translator = null,
    inker = null,
    colorist = null,
    letterer = null,
    coverArtist = null,
    tags = null,
    year = null,
    month = null,
    day = null,
    categories = null,
    source = null,
)

internal fun LocalEntryMetadata.copyFromComicInfo(comicInfo: ComicInfo) {
    comicInfo.series?.let { title = it.value }
    comicInfo.writer?.let { author = it.value }
    comicInfo.summary?.let { description = it.value }

    listOfNotNull(
        comicInfo.genre?.value,
        comicInfo.tags?.value,
        comicInfo.categories?.value,
    )
        .flatMap { it.split(", ") }
        .distinct()
        .map { it.trim() }
        .filterNot { it.isBlank() }
        .takeIf { it.isNotEmpty() }
        ?.let { genre = it }

    listOfNotNull(
        comicInfo.penciller?.value,
        comicInfo.inker?.value,
        comicInfo.colorist?.value,
        comicInfo.letterer?.value,
        comicInfo.coverArtist?.value,
    )
        .flatMap { it.split(", ") }
        .distinct()
        .joinToString(", ") { it.trim() }
        .takeIf { it.isNotEmpty() }
        ?.let { artist = it }

    status = toEntryStatus(comicInfo.publishingStatus?.value)
}

internal fun LocalEntryChapterMetadata.copyFromComicInfo(comicInfo: ComicInfo) {
    comicInfo.title?.let { name = it.value }
    comicInfo.number?.value?.toDoubleOrNull()?.let { chapterNumber = it }
    comicInfo.translator?.let { scanlator = it.value }
    comicInfo.uploadDateMillis()?.let { dateUpload = it }
}

private fun toComicInfoPublishingStatus(status: Int): String {
    return when (status) {
        LocalEntryMetadata.ONGOING -> "Ongoing"
        LocalEntryMetadata.COMPLETED -> "Completed"
        LocalEntryMetadata.LICENSED -> "Licensed"
        LocalEntryMetadata.PUBLISHING_FINISHED -> "Publishing finished"
        LocalEntryMetadata.CANCELLED -> "Cancelled"
        LocalEntryMetadata.ON_HIATUS -> "On hiatus"
        else -> "Unknown"
    }
}

private fun toEntryStatus(status: String?): Int {
    return when (status) {
        "Ongoing" -> LocalEntryMetadata.ONGOING
        "Completed" -> LocalEntryMetadata.COMPLETED
        "Licensed" -> LocalEntryMetadata.LICENSED
        "Publishing finished" -> LocalEntryMetadata.PUBLISHING_FINISHED
        "Cancelled" -> LocalEntryMetadata.CANCELLED
        "On hiatus" -> LocalEntryMetadata.ON_HIATUS
        else -> LocalEntryMetadata.UNKNOWN
    }
}
