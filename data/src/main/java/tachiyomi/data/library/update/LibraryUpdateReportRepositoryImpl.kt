package tachiyomi.data.library.update

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.domain.library.update.model.LibraryUpdateRun
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryUpdateReportRepositoryImpl(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : LibraryUpdateReportRepository {

    override fun subscribeLatestRun(): Flow<LibraryUpdateRun?> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToOneOrNull { library_update_runsQueries.getByProfile(profileId, ::mapRun) }
        }
    }

    override suspend fun getLatestRun(): LibraryUpdateRun? {
        return handler.awaitOneOrNull {
            library_update_runsQueries.getByProfile(profileProvider.activeProfileId, ::mapRun)
        }
    }

    override fun subscribeLatestRunStatuses(): Flow<List<EntryUpdateStatus>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToOneOrNull { library_update_runsQueries.getByProfile(profileId, ::mapRun) }
                .flatMapLatest { run ->
                    if (run == null) {
                        flowOf(emptyList())
                    } else {
                        handler.subscribeToList {
                            entry_update_statusQueries.getDecidedSince(profileId, run.startedAt, ::mapStatus)
                        }
                    }
                }
        }
    }

    override fun subscribeStatus(entryId: Long): Flow<EntryUpdateStatus?> {
        return handler.subscribeToOneOrNull { entry_update_statusQueries.getByEntryId(entryId, ::mapStatus) }
    }

    override suspend fun getStatuses(): Map<Long, EntryUpdateStatus> {
        return handler.awaitList {
            entry_update_statusQueries.getByProfile(profileProvider.activeProfileId, ::mapStatus)
        }
            .associateBy(EntryUpdateStatus::entryId)
    }

    override suspend fun startRun(startedAt: Long, trigger: LibraryUpdateTrigger, librarySize: Int) {
        handler.await {
            library_update_runsQueries.start(
                profileId = profileProvider.activeProfileId,
                startedAt = startedAt,
                trigger = trigger.name.lowercase(),
                librarySize = librarySize.toLong(),
            )
        }
    }

    override suspend fun finishRun(startedAt: Long, finishedAt: Long) {
        handler.await {
            library_update_runsQueries.finish(
                finishedAt = finishedAt,
                profileId = profileProvider.activeProfileId,
                startedAt = startedAt,
            )
        }
    }

    override suspend fun recordDecisions(decidedAt: Long, decisions: List<LibraryUpdateReportRepository.Decision>) {
        handler.await(inTransaction = true) {
            decisions.forEach { decision ->
                entry_update_statusQueries.upsertDecision(
                    entryId = decision.entryId,
                    decidedAt = decidedAt,
                    outcome = decision.reason.outcome.name.lowercase(),
                    reason = decision.reason.name.lowercase(),
                    reasonCategoryId = decision.reasonCategoryId,
                )
            }
        }
    }

    override suspend fun recordChecked(entryId: Long, decidedAt: Long, checkedAt: Long, newChapters: Int) {
        val outcome = if (newChapters > 0) EntryUpdateOutcome.NEW_CHAPTERS else EntryUpdateOutcome.NO_CHANGES
        handler.await {
            entry_update_statusQueries.recordChecked(
                entryId = entryId,
                decidedAt = decidedAt,
                outcome = outcome.name.lowercase(),
                newChapters = newChapters.toLong(),
                checkedAt = checkedAt,
            )
        }
    }

    override suspend fun recordFailed(entryId: Long, decidedAt: Long, error: String?) {
        handler.await {
            entry_update_statusQueries.recordFailed(
                entryId = entryId,
                decidedAt = decidedAt,
                error = error,
            )
        }
    }

    private fun mapRun(
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

    private fun mapStatus(
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
}
