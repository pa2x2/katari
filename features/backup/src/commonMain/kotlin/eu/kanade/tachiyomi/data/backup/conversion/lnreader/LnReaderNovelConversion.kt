package eu.kanade.tachiyomi.data.backup.conversion.lnreader

import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupEntryProgressState
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.source.lnreader.lnReaderReleaseTime
import tachiyomi.domain.source.lnreader.lnReaderStatus
import java.time.ZoneId

/**
 * The novel as a Katari book from [source], in the categories of [categories] orders. Details are read as Katari's
 * LNReader source reads them; the cover isn't carried, as the backup holds only LNReader's own copy of it (D080), so
 * the entry is left to fetch its details. Chapter times without a zone are read in [zone].
 */
internal fun LnReaderNovel.toBackupEntry(source: Long, categories: List<Long>, zone: ZoneId): BackupEntry {
    val newest = chapters.maxOfOrNull { it.position } ?: 0
    val reading = LnReaderReading(this, zone)
    return BackupEntry(
        source = source,
        url = path,
        title = name,
        artist = artist.text(),
        author = author.text(),
        description = summary.text(),
        genre = genres.orEmpty().split(',').map(String::trim).filter(String::isNotEmpty).distinct(),
        status = lnReaderStatus(status).value,
        chapters = chapters.map { it.toBackupChapter(newest, zone) },
        categories = categories,
        favorite = inLibrary,
        history = reading.history,
        progressStates = chapters.mapNotNull { it.progressState(reading.readAt(it)) },
        activitySessions = reading.sessions,
        type = EntryType.BOOK,
    )
}

/** Whether the novel holds reading state worth carrying even outside the library. */
internal val LnReaderNovel.hasProgress: Boolean
    get() = chapters.any { !it.unread || it.bookmark || it.readTime != null || (it.progress ?: 0) > 0 }

// Katari lists chapters newest first, LNReader counts its positions from the oldest
private fun LnReaderChapter.toBackupChapter(newest: Long, zone: ZoneId): BackupChapter {
    val released = lnReaderReleaseTime(releaseTime, zone)
    return BackupChapter(
        url = path,
        name = name.trim().ifEmpty { "Chapter ${position + 1}" },
        scanlator = scanlator?.takeIf(String::isNotBlank),
        read = !unread,
        bookmark = bookmark,
        dateUpload = released?.date ?: 0,
        dateText = released?.text,
        chapterNumber = chapterNumber?.takeIf { it >= 0 } ?: -1.0,
        sourceOrder = newest - position,
    )
}

/** Where a chapter read part of the way stopped, as a place in the chapter Katari's reader opens at (D079). */
private fun LnReaderChapter.progressState(readAt: Long?): BackupEntryProgressState? {
    val percent = progress?.takeIf { it in 1..99 } ?: return null
    val progression = percent / 100.0
    return BackupEntryProgressState(
        resourceKey = path,
        sourceChildKey = path,
        locatorKind = BOOK_LOCATOR_KIND,
        progression = progression,
        totalProgression = progression,
        locatorUpdatedAt = readAt ?: 0,
    )
}

private fun String?.text(): String? = this?.trim()?.takeIf(String::isNotEmpty)

// Katari's book reader's progress locator kind (BookProgress.kt in the book reader)
private const val BOOK_LOCATOR_KIND = "book"
