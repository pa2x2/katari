package tachiyomi.domain.entry.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.preference.SourcePreferenceProfile
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.repository.EntryChapterListWalkRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.work.WorkBackoff
import tachiyomi.domain.work.WorkOutcome
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.minutes

/**
 * Reads the older parts of every chapter list still arriving in parts, of every profile: sources side by side, one
 * title at a time per source, so no site gets more than one walk at once. A walk that fails keeps its reason for the
 * chapter list, and the pass asks to be retried with [BACKOFF], up to [MAX_ATTEMPTS] runs.
 */
@Inject
class WalkPendingChapterLists(
    private val walks: EntryChapterListWalkRepository,
    private val entryRepository: EntryRepository,
    private val finishChapterListWalk: FinishChapterListWalk,
) {
    /** @param attempt how many times the pass ran before, 0 on its first run. */
    @OptIn(ExperimentalAtomicApi::class)
    suspend fun await(attempt: Int): WorkOutcome {
        val attempted = mutableSetOf<Long>()
        val failed = AtomicBoolean(false)
        // Walks started while this run goes on are picked up before it ends
        while (true) {
            val pending = walks.getAll().filter { it.entryId !in attempted }
            if (pending.isEmpty()) break
            attempted += pending.map { it.entryId }
            val entries = pending.mapNotNull { walk ->
                entryRepository.getEntryById(walk.entryId, walk.profileId)
                    ?: null.also { walks.finish(walk.entryId) }
            }
            coroutineScope {
                entries.groupBy { it.source }.values.forEach { sourceEntries ->
                    launch {
                        sourceEntries.forEach { entry ->
                            try {
                                // The source reads its settings (domain, mirror, login) from the entry's profile,
                                // whichever one is active
                                withContext(SourcePreferenceProfile.of(entry.profileId)) {
                                    finishChapterListWalk(entry)
                                }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                logcat(LogPriority.WARN, error) {
                                    "Older chapters of entry ${entry.id} stopped loading"
                                }
                                failed.store(true)
                            }
                        }
                    }
                }
            }
        }
        return when {
            !failed.load() -> WorkOutcome.Done
            // The walks keep their failure on the chapter list, where the user can retry them
            attempt >= MAX_ATTEMPTS -> WorkOutcome.Done
            else -> WorkOutcome.RetryLater
        }
    }

    companion object {
        const val MAX_ATTEMPTS = 8

        val BACKOFF = WorkBackoff(initialDelay = 1.minutes)
    }
}
