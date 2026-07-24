package eu.kanade.tachiyomi.ui.download

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryDownloadPresentation
import mihon.entry.interactions.EntryDownloadQueueItem
import mihon.entry.interactions.description
import mihon.entry.interactions.resolve

data class DownloadQueueHeaderModel(
    val id: Long,
    val entryType: EntryType,
    val title: String,
    val count: Int,
) {
    val displayTitle: String
        get() = "$title ($count)"
}

data class DownloadQueueItemModel(
    val id: Long,
    val entryType: EntryType,
    val title: String,
    val subtitle: String,
    val progress: Int,
    val progressMax: Int,
    val presentation: EntryDownloadPresentation,
) {
    fun progressText(context: Context): String = presentation.description()?.resolve(context).orEmpty()
}

fun EntryDownloadQueueItem.toDownloadQueueItemModel(): DownloadQueueItemModel {
    return DownloadQueueItemModel(
        id = childId,
        entryType = entryType,
        title = title,
        subtitle = subtitle,
        progress = progress,
        progressMax = progressMax,
        presentation = presentation,
    )
}
