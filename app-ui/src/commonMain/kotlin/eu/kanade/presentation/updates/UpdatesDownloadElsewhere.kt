package eu.kanade.presentation.updates

import androidx.compose.runtime.Composable
import app.katari.sync.client.downloadrequest.ChapterToRequest
import app.katari.sync.ui.download.DownloadElsewhereRequest
import app.katari.sync.ui.download.chaptersElsewhere
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import tachiyomi.domain.updates.model.UpdateItem

/** Asking another device to download the chapters of [items] that can be downloaded; null when none can. */
@Composable
internal fun updatesDownloadElsewhere(items: List<UpdatesItem>): DownloadElsewhereRequest.Chapters? {
    val downloadable = items.filter { it.downloadAvailable && it.update is UpdateItem.EntryUpdate }
    if (downloadable.isEmpty()) return null
    val updates = downloadable.map { (it.update as UpdateItem.EntryUpdate).update }
    return chaptersElsewhere(
        chapters = updates.map { ChapterToRequest(it.entryId, it.chapterUrl, it.chapterName) },
        sourceId = updates.mapTo(mutableSetOf()) { it.sourceId }.singleOrNull(),
        entryTitle = downloadable.mapTo(mutableSetOf()) { it.visibleEntryTitle }.singleOrNull(),
        presentation = updates.mapTo(mutableSetOf()) { it.entryType }.singleOrNull().entryTypePresentation(),
    )
}
