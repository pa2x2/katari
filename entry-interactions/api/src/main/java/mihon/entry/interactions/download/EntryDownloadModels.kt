package mihon.entry.interactions

import android.content.Context
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.MR

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
) {
    init {
        require(items.all { it.entryType == entryType && it.sourceId == sourceId }) {
            "Download queue group must contain only $entryType items from source $sourceId"
        }
    }
}

data class EntryDownloadIdentity(
    val profileId: Long,
    val entryType: EntryType,
    val entryId: Long,
    val sourceId: Long,
    val childId: Long,
) {
    companion object {
        fun from(entry: Entry, child: EntryChapter): EntryDownloadIdentity {
            require(child.entryId == entry.id) {
                "Download child ${child.id} belongs to entry ${child.entryId}, not ${entry.id}"
            }
            return EntryDownloadIdentity(
                profileId = entry.profileId,
                entryType = entry.type,
                entryId = entry.id,
                sourceId = entry.source,
                childId = child.id,
            )
        }
    }
}

data class EntryDownloadEntryIdentity(
    val profileId: Long,
    val entryType: EntryType,
    val entryId: Long,
) {
    companion object {
        fun from(entry: Entry) = EntryDownloadEntryIdentity(
            profileId = entry.profileId,
            entryType = entry.type,
            entryId = entry.id,
        )
    }
}

data class EntryDownloadQueueItem(
    val identity: EntryDownloadIdentity,
    val state: EntryDownloadState,
    val title: String,
    val subtitle: String,
    val dateUpload: Long,
    val chapterNumber: Double,
    val progress: Int,
    val progressMax: Int,
    val presentation: EntryDownloadPresentation = EntryDownloadPresentation.forState(state),
) {
    val profileId: Long get() = identity.profileId
    val entryType: EntryType get() = identity.entryType
    val entryId: Long get() = identity.entryId
    val sourceId: Long get() = identity.sourceId
    val childId: Long get() = identity.childId

    init {
        require(progressMax > 0 && progress in 0..progressMax)
        require(presentation.phase.accepts(state)) {
            "Download phase ${presentation.phase} does not match queue state $state"
        }
    }
}

enum class EntryDownloadPhase {
    IDLE,
    QUEUED,
    RESOLVING,
    TRANSFERRING,
    FINALIZING,
    COMPLETED,
    FAILED,
    ;

    internal fun accepts(state: EntryDownloadState): Boolean = when (state) {
        EntryDownloadState.NOT_DOWNLOADED -> this == IDLE
        EntryDownloadState.QUEUE -> this == QUEUED
        EntryDownloadState.DOWNLOADING -> this == RESOLVING || this == TRANSFERRING || this == FINALIZING
        EntryDownloadState.DOWNLOADED -> this == COMPLETED
        EntryDownloadState.ERROR -> this == FAILED
    }
}

sealed interface EntryDownloadProgress {
    data object None : EntryDownloadProgress

    data class Percent(val value: Int) : EntryDownloadProgress {
        init {
            require(value in 0..100)
        }
    }

    data class Units(val completed: Int, val total: Int) : EntryDownloadProgress {
        init {
            require(total > 0)
            require(completed in 0..total)
        }
    }
}

data class EntryDownloadPresentation(
    val phase: EntryDownloadPhase,
    val progress: EntryDownloadProgress = EntryDownloadProgress.None,
    val failure: EntryDownloadMessage? = null,
) {
    init {
        require(failure == null || phase == EntryDownloadPhase.FAILED)
        require(failure == null || failure is EntryDownloadMessage.Resource) {
            "Queue failures must use a localized download message"
        }
        require(progress == EntryDownloadProgress.None || phase == EntryDownloadPhase.TRANSFERRING)
    }

    companion object {
        fun forState(state: EntryDownloadState): EntryDownloadPresentation = EntryDownloadPresentation(
            phase = when (state) {
                EntryDownloadState.NOT_DOWNLOADED -> EntryDownloadPhase.IDLE
                EntryDownloadState.QUEUE -> EntryDownloadPhase.QUEUED
                EntryDownloadState.DOWNLOADING -> EntryDownloadPhase.TRANSFERRING
                EntryDownloadState.DOWNLOADED -> EntryDownloadPhase.COMPLETED
                EntryDownloadState.ERROR -> EntryDownloadPhase.FAILED
            },
        )
    }
}

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
        val entryIdentity: EntryDownloadEntryIdentity?,
        val title: String?,
        val subtitle: String?,
        val message: EntryDownloadMessage,
    ) : EntryDownloadEvent {
        init {
            require(entryIdentity == null || entryIdentity.entryType == entryType) {
                "Download error identity type must match the event type"
            }
        }
    }

    data class Warning(
        val message: EntryDownloadMessage,
        val timeoutMillis: Long? = null,
        val helpUrl: String? = null,
    ) : EntryDownloadEvent
}

fun EntryDownloadPresentation.description(): EntryDownloadMessage? = when (phase) {
    EntryDownloadPhase.IDLE -> null
    EntryDownloadPhase.QUEUED -> EntryDownloadMessage.Resource(MR.strings.download_state_queued)
    EntryDownloadPhase.RESOLVING -> EntryDownloadMessage.Resource(MR.strings.download_state_resolving)
    EntryDownloadPhase.TRANSFERRING -> when (val progress = progress) {
        EntryDownloadProgress.None -> EntryDownloadMessage.Resource(MR.strings.download_state_downloading)
        is EntryDownloadProgress.Percent -> EntryDownloadMessage.Resource(
            MR.strings.download_progress_percent,
            listOf(progress.value),
        )
        is EntryDownloadProgress.Units -> EntryDownloadMessage.Resource(
            MR.strings.download_progress_units,
            listOf(progress.completed, progress.total),
        )
    }
    EntryDownloadPhase.FINALIZING -> EntryDownloadMessage.Resource(MR.strings.download_state_finalizing)
    EntryDownloadPhase.COMPLETED -> EntryDownloadMessage.Resource(MR.strings.label_downloaded)
    EntryDownloadPhase.FAILED -> failure ?: EntryDownloadMessage.Resource(MR.strings.chapter_error)
}

fun EntryDownloadMessage.resolve(context: Context): String = when (this) {
    is EntryDownloadMessage.Text -> value
    is EntryDownloadMessage.Resource -> context.stringResource(resource, *args.toTypedArray())
}
