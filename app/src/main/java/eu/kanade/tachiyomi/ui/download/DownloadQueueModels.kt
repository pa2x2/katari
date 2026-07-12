package eu.kanade.tachiyomi.ui.download

import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryDownloadQueueItem

data class DownloadQueueContentType(
    val numberSortLabel: StringResource,
)

data class DownloadQueueHeaderModel(
    val id: Long,
    val contentType: DownloadQueueContentType,
    val title: String,
    val count: Int,
) {
    val displayTitle: String
        get() = "$title ($count)"
}

data class DownloadQueueItemModel(
    val id: Long,
    val contentType: DownloadQueueContentType,
    val title: String,
    val subtitle: String,
    val progress: Int,
    val progressMax: Int,
    val progressText: String,
)

fun EntryDownloadQueueItem.toDownloadQueueItemModel(): DownloadQueueItemModel {
    return DownloadQueueItemModel(
        id = childId,
        contentType = entryType.toDownloadQueueContentType(),
        title = title,
        subtitle = subtitle,
        progress = progress,
        progressMax = progressMax,
        progressText = progressText,
    )
}

fun EntryType.toDownloadQueueContentType(): DownloadQueueContentType {
    return DownloadQueueContentType(
        numberSortLabel = entryTypePresentation().downloadNumberSortLabel,
    )
}
