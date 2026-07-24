package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType

/** Feature-owned Automatic Download applicability projected from contributed Download support. */
interface EntryAutomaticDownloadFeature {
    fun isApplicable(type: EntryType): Boolean
}

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
