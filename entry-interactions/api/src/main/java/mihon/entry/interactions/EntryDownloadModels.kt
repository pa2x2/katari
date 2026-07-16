package mihon.entry.interactions

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryType

enum class EntryDownloadState(val value: Int) {
    NOT_DOWNLOADED(0),
    QUEUE(1),
    DOWNLOADING(2),
    DOWNLOADED(3),
    ERROR(4),
}

data class EntryDownloadStatus(
    val entryType: EntryType,
    val chapterId: Long,
    val state: EntryDownloadState,
    val progress: Int = 0,
)

data class EntryDownloadQueueGroup(
    val sourceId: Long,
    val sourceName: String,
    val entryType: EntryType,
    val items: List<EntryDownloadQueueItem>,
)

data class EntryDownloadQueueItem(
    val entryType: EntryType,
    val state: EntryDownloadState,
    val entryId: Long,
    val childId: Long,
    val title: String,
    val subtitle: String,
    val dateUpload: Long,
    val chapterNumber: Double,
    val progress: Int,
    val progressMax: Int,
    val progressText: String,
)

sealed interface EntryDownloadMessage {
    data class Text(val value: String) : EntryDownloadMessage

    data class Resource(
        val resource: StringResource,
        val args: List<Any> = emptyList(),
    ) : EntryDownloadMessage
}

sealed interface EntryDownloadEvent {
    data class Error(
        val entryType: EntryType,
        val entryId: Long?,
        val title: String?,
        val subtitle: String?,
        val message: EntryDownloadMessage,
    ) : EntryDownloadEvent

    data class Warning(
        val message: EntryDownloadMessage,
        val timeoutMillis: Long? = null,
        val helpUrl: String? = null,
    ) : EntryDownloadEvent
}
