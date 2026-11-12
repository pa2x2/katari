package eu.kanade.presentation.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import app.katari.sync.client.downloadrequest.ChapterToRequest
import app.katari.sync.ui.download.DownloadElsewhereRequest
import app.katari.sync.ui.download.chaptersElsewhere
import eu.kanade.tachiyomi.ui.entry.chapters.EntryChapterList
import tachiyomi.domain.entry.model.EntryChapter

/**
 * What an entry's Download menu needs to offer its amounts to another device: the entry and its merged members
 * ([entryIds]), the source of their chapters (null for several), its [title], and the chapters each amount picks.
 */
@Immutable
class EntryDownloadElsewhere(
    val entryIds: Set<Long>,
    val sourceId: Long?,
    val title: String,
    val chapters: suspend (DownloadAction) -> List<ChapterToRequest>,
)

/** Asking another device to download the chapters of [items]. */
@Composable
fun entryChaptersElsewhere(items: List<EntryChapterList.Item>): DownloadElsewhereRequest.Chapters = chaptersElsewhere(
    chapters = items.map { it.chapter.toRequest() },
    sourceId = items.mapTo(mutableSetOf()) { it.entry.source }.singleOrNull(),
    entryTitle = items.mapTo(mutableSetOf()) { it.entry.title }.singleOrNull(),
    presentation = items.mapTo(mutableSetOf()) { it.entry.type }.singleOrNull().entryTypePresentation(),
)

fun EntryChapter.toRequest(): ChapterToRequest = ChapterToRequest(entryId, url, name)
