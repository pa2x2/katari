package tachiyomi.data.library.update

import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.domain.library.update.model.LibraryUpdateRun
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger

fun libraryUpdateRun(
    startedAt: Long,
    finishedAt: Long?,
    trigger: String,
    librarySize: Long,
): LibraryUpdateRun = LibraryUpdateRun(
    startedAt = startedAt,
    finishedAt = finishedAt,
    trigger = LibraryUpdateTrigger.valueOf(trigger.uppercase()),
    librarySize = librarySize.toInt(),
)

fun entryUpdateStatus(
    entryId: Long,
    decidedAt: Long,
    outcome: String,
    reason: String?,
    reasonCategoryId: Long?,
    error: String?,
    newChapters: Long,
    lastCheckedAt: Long?,
    consecutiveFailures: Long,
): EntryUpdateStatus = EntryUpdateStatus(
    entryId = entryId,
    decidedAt = decidedAt,
    outcome = EntryUpdateOutcome.valueOf(outcome.uppercase()),
    reason = reason?.let { EntryUpdateDecisionReason.valueOf(it.uppercase()) },
    reasonCategoryId = reasonCategoryId,
    error = error,
    newChapters = newChapters.toInt(),
    lastCheckedAt = lastCheckedAt,
    consecutiveFailures = consecutiveFailures.toInt(),
)
