package eu.kanade.tachiyomi.data.library.queue

import app.katari.profiles.core.ProfileDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.library.LibraryUpdateInput
import eu.kanade.tachiyomi.data.library.LibraryUpdateSelectionFile
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryUpdateQueue
import tachiyomi.domain.library.update.model.LibraryUpdateStart
import tachiyomi.domain.library.update.model.LibraryUpdateState

/**
 * The [LibraryUpdateQueue] as one [LibraryUpdateWorkChain]: a new update goes behind every other, so profiles update
 * one after another, and a profile that already has an update running or waiting gets no second one.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class WorkChainLibraryUpdateQueue(
    private val chain: LibraryUpdateWorkChain,
    private val selectionFile: LibraryUpdateSelectionFile,
    private val profileDatabase: ProfileDatabase,
    private val activeProfileProvider: ActiveProfileProvider,
) : LibraryUpdateQueue {
    override val isOffered: Boolean = true

    // Between reading the chain and adding to it, another start could add the same profile's update.
    private val enqueueLock = Mutex()

    override suspend fun startNow(category: Category?, sourceId: Long?, entryType: EntryType?): LibraryUpdateStart {
        return enqueue(
            LibraryUpdateInput(
                profileId = activeProfileProvider.activeProfileId,
                categoryId = category?.id,
                sourceId = sourceId,
                entryType = entryType,
            ),
        )
    }

    override suspend fun startSelection(entryIds: Collection<Long>): LibraryUpdateStart {
        val profileId = activeProfileProvider.activeProfileId
        // Checked before writing the selection, which a large library makes worth skipping.
        if (chain.currentUpdates().any { it.profileId == profileId }) return LibraryUpdateStart.AlreadyQueued
        val selectionName = selectionFile.write(entryIds)
        return enqueue(LibraryUpdateInput(profileId = profileId, selectionName = selectionName))
            .also { start ->
                if (start == LibraryUpdateStart.AlreadyQueued) selectionFile.delete(selectionName)
            }
    }

    override suspend fun startSkippedOfLatest(): LibraryUpdateStart {
        return enqueue(LibraryUpdateInput(profileId = activeProfileProvider.activeProfileId, skippedOfLatest = true))
    }

    override suspend fun startAutomatic(profileId: Long): LibraryUpdateStart {
        return enqueue(LibraryUpdateInput(profileId = profileId, automatic = true))
    }

    override suspend fun stop() {
        stop(activeProfileProvider.activeProfileId)
    }

    override suspend fun stop(profileId: Long) {
        chain.currentUpdates()
            .filter { it.profileId == profileId }
            .forEach { chain.remove(it) }
    }

    override fun stateFlow(profileId: Long): Flow<LibraryUpdateState?> {
        return chain.updates
            .map { updates ->
                val own = updates.firstOrNull { it.profileId == profileId } ?: return@map null
                if (own.isRunning) {
                    own.progress?.let(LibraryUpdateState::Running)
                } else {
                    updates.firstOrNull(ChainedLibraryUpdate::isRunning)
                        ?.profileId
                        ?.let { profileName(it) }
                        ?.let(LibraryUpdateState::Waiting)
                }
            }
            .distinctUntilChanged()
    }

    /** Profile of the update that starts after the update [running], or null when none waits. */
    fun nextProfileFlow(running: String): Flow<Long?> {
        return chain.updates
            .map { updates ->
                updates.filter { it.id != running }.minByOrNull(ChainedLibraryUpdate::queuedAt)?.profileId
            }
            .distinctUntilChanged()
    }

    private suspend fun enqueue(input: LibraryUpdateInput): LibraryUpdateStart = enqueueLock.withLock {
        val pending = chain.currentUpdates()
        if (pending.any { it.profileId == input.profileId }) return LibraryUpdateStart.AlreadyQueued
        chain.append(input)
        pending.maxByOrNull(ChainedLibraryUpdate::queuedAt)
            ?.profileId
            ?.let { profileName(it) }
            ?.let(LibraryUpdateStart::Queued)
            ?: LibraryUpdateStart.Started
    }

    private suspend fun profileName(profileId: Long): String? {
        return profileDatabase.getProfileById(profileId)?.name
    }
}
