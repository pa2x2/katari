package mihon.entry.interactions.download

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned Automatic Download applicability projected from contributed Download support, and what it queues. */
interface EntryAutomaticDownloadFeature {
    fun isApplicable(type: EntryType): Boolean

    /** Chapters as they are queued for download automatically, for features that follow them up. */
    val scheduled: Flow<EntryAutomaticDownloadScheduled>
}

data class EntryAutomaticDownloadScheduled(
    val entry: Entry,
    val chapters: List<EntryChapter>,
)

sealed interface EntryAutomaticDownloadResult {
    data class Inapplicable(
        val type: EntryType,
    ) : EntryAutomaticDownloadResult

    data class Blocked(
        val blockers: Set<EntryAutomaticDownloadBlocker>,
    ) : EntryAutomaticDownloadResult

    data class Scheduled(val count: Int) : EntryAutomaticDownloadResult
}

enum class EntryAutomaticDownloadBlocker {
    EMPTY_SELECTION,
    DISABLED,
    ENTRY_NOT_IN_LIBRARY,
    CATEGORY_POLICY_REJECTED,
    NO_UNREAD_CANDIDATES,
}
