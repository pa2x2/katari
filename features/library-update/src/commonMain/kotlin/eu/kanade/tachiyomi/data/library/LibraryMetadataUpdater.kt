package eu.kanade.tachiyomi.data.library

import app.katari.entry.interactions.merge.EntryMergeMetadataRefreshFeature
import app.katari.entry.interactions.source.EntrySourceRefreshFailure
import app.katari.entry.interactions.source.EntrySourceRefreshFeature
import app.katari.entry.interactions.source.EntrySourceRefreshRequest
import app.katari.entry.interactions.source.EntrySourceRefreshResult
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotifications
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.preference.SourcePreferenceProfile
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.work.WorkOutcome
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

/**
 * Refreshes the details and covers of every entry in a profile's library, without their chapter lists: sources side by
 * side and each source one entry at a time. One serves a single [update], since its [notifications] keep that
 * refresh's progress.
 */
@OptIn(ExperimentalAtomicApi::class)
@Inject
class LibraryMetadataUpdater(
    private val getLibraryEntries: GetLibraryEntries,
    private val entryRepository: EntryRepository,
    private val sourceRefreshFeature: EntrySourceRefreshFeature,
    private val mergeMetadataRefreshFeature: EntryMergeMetadataRefreshFeature,
    private val notifications: LibraryUpdateNotifications,
) {
    /**
     * Refreshes the library of [profileId], which stays the profile refreshed when another is opened meanwhile. A
     * refresh that is stopped counts as done.
     */
    suspend fun update(profileId: Long): WorkOutcome {
        val entries = entriesToUpdate(profileId)

        // Extensions read logins and API keys from this profile's source preferences, not the active profile's.
        return withContext(Dispatchers.IO + SourcePreferenceProfile.of(profileId)) {
            try {
                updateMetadata(profileId, entries)
                WorkOutcome.Done
            } catch (e: Exception) {
                if (e is CancellationException) {
                    WorkOutcome.Done
                } else {
                    logcat(LogPriority.ERROR, e)
                    WorkOutcome.Failed
                }
            } finally {
                notifications.cancelProgress()
            }
        }
    }

    private suspend fun entriesToUpdate(profileId: Long): List<Entry> {
        val libraryItems = getLibraryEntries.await(profileId)
        notifications.showQueueSizeWarningIfNeeded(profileId, libraryItems, rulesProfileId = null)
        return libraryItems.expandToMemberEntries()
    }

    private suspend fun List<LibraryItem>.expandToMemberEntries(): List<Entry> {
        return flatMap { libraryItem ->
            mergeMetadataRefreshFeature.resolveOwners(libraryItem.entry).orderedOwners
        }
            .distinctBy(Entry::id)
    }

    private suspend fun updateMetadata(profileId: Long, entriesToUpdate: List<Entry>) {
        val semaphore = Semaphore(5)
        val progressCount = AtomicInt(0)
        val currentlyUpdatingEntries = CopyOnWriteArrayList<Entry>()

        coroutineScope {
            entriesToUpdate.groupBy { it.source }
                .values
                .map { entriesInSource ->
                    async {
                        semaphore.withPermit {
                            entriesInSource.forEach { entry ->
                                ensureActive()

                                withUpdateNotification(
                                    profileId,
                                    currentlyUpdatingEntries,
                                    progressCount,
                                    entriesToUpdate.size,
                                    entry,
                                ) {
                                    refresh(profileId, entry)
                                }
                            }
                        }
                    }
                }
                .awaitAll()
        }

        notifications.cancelProgress()
    }

    private suspend fun refresh(profileId: Long, entry: Entry) {
        val freshEntry = entryRepository.getEntryById(entry.id, profileId)
        if (freshEntry?.favorite != true) return
        try {
            when (
                val result = sourceRefreshFeature.refresh(
                    EntrySourceRefreshRequest(
                        entry = freshEntry,
                        fetchDetails = true,
                        fetchChildren = false,
                        manual = false,
                    ),
                )
            ) {
                is EntrySourceRefreshResult.Refreshed -> Unit
                is EntrySourceRefreshResult.SourceUnavailable -> logcat(LogPriority.ERROR) {
                    "Metadata refresh source ${result.sourceId} is unavailable"
                }
                is EntrySourceRefreshResult.Failed -> when (val reason = result.reason) {
                    EntrySourceRefreshFailure.NoChildren -> Unit
                    is EntrySourceRefreshFailure.Operation -> throw reason.error
                }
            }
        } catch (e: Throwable) {
            // Ignore errors and continue
            logcat(LogPriority.ERROR, e)
        }
    }

    private suspend fun withUpdateNotification(
        profileId: Long,
        updatingEntries: CopyOnWriteArrayList<Entry>,
        completed: AtomicInt,
        total: Int,
        entry: Entry,
        block: suspend () -> Unit,
    ) = coroutineScope {
        ensureActive()

        updatingEntries.add(entry)
        notifications.showMetadataProgress(profileId, updatingEntries, completed.load(), total)

        block()

        ensureActive()

        updatingEntries.remove(entry)
        completed.fetchAndIncrement()
        notifications.showMetadataProgress(profileId, updatingEntries, completed.load(), total)
    }
}
