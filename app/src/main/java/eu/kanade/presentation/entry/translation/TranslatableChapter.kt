package eu.kanade.presentation.entry.translation

import mihon.entry.interactions.download.EntryDownloadState
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** A chapter a translation action applies to, with the download state it is shown with. */
data class TranslatableChapter(
    val entry: Entry,
    val chapter: EntryChapter,
    val downloadState: EntryDownloadState,
) {
    val id: Long
        get() = chapter.id
}
