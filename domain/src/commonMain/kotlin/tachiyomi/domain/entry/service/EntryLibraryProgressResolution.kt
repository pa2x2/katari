package tachiyomi.domain.entry.service

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Domain-side port used while constructing unified Library items.
 *
 * The application-facing implementation is owned by the Entry Library Progress feature. Keeping this neutral port in
 * Domain avoids reversing the existing `entry-interactions:api -> domain` dependency.
 */
interface EntryLibraryProgressResolutionPort {
    suspend fun calculate(
        entry: Entry,
        chapters: List<EntryChapter>,
        lastRead: Long,
    ): EntryLibraryProgressResolution

    suspend fun calculateBatch(
        members: List<EntryLibraryProgressMember>,
    ): Map<Long, EntryLibraryProgressResolution> {
        return buildMap {
            members.forEach { member ->
                put(
                    member.entry.id,
                    calculate(member.entry, member.chapters, member.lastRead),
                )
            }
        }
    }

    fun merge(
        entryType: EntryType,
        members: List<EntryLibraryProgressSummary>,
    ): EntryLibraryProgressResolution
}

data class EntryLibraryProgressMember(
    val entry: Entry,
    val chapters: List<EntryChapter>,
    val lastRead: Long,
)

sealed interface EntryLibraryProgressResolution {
    data class Available(val summary: EntryLibraryProgressSummary) : EntryLibraryProgressResolution
    data class Inapplicable(val type: EntryType) : EntryLibraryProgressResolution
}

data class EntryLibraryProgressSummary(
    val totalCount: Long,
    val consumedCount: Long,
    val hasStarted: Boolean,
    val bookmarkCount: Long?,
    val inProgressItemId: Long?,
    val inProgressFraction: Float?,
    val lastRead: Long,
    val continueTarget: EntryLibraryContinueTarget,
) {
    val unconsumedCount: Long
        get() = totalCount - consumedCount
}

sealed interface EntryLibraryContinueTarget {
    data object Inapplicable : EntryLibraryContinueTarget
    data object NoNext : EntryLibraryContinueTarget
    data class Available(val childId: Long) : EntryLibraryContinueTarget
}
