package mihon.entry.interactions

import kotlinx.coroutines.flow.Flow

/** Purpose-specific visible-Entry resolution for navigation and notification destinations. */
interface EntryMergeNavigationFeature {
    suspend fun resolveNavigation(subject: EntryMergeSubject): EntryMergeNavigationProjection

    /** Compatibility path for installed notification payloads that contain only a globally unique Entry ID. */
    suspend fun resolveLegacyNotification(entryId: Long): EntryMergeNavigationProjection?

    fun observeNavigation(subject: EntryMergeSubject): Flow<EntryMergeNavigationProjection>
}

data class EntryMergeNavigationProjection(
    val requestedSubject: EntryMergeSubject,
    val visibleEntryId: Long,
)
