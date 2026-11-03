package mihon.desktop.library

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.library.LibraryUpdateInput
import eu.kanade.tachiyomi.data.library.LibraryUpdateRun
import eu.kanade.tachiyomi.data.library.queue.ChainedLibraryUpdate
import eu.kanade.tachiyomi.data.library.queue.LibraryUpdateWorkChain
import eu.kanade.tachiyomi.data.library.queue.WorkChainLibraryUpdateQueue
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import mihon.desktop.work.ExistingWorkPolicy
import mihon.desktop.work.InProcessWork
import mihon.desktop.work.InProcessWorkInfo
import mihon.desktop.work.InProcessWorkRequest
import mihon.desktop.work.ResumableWork
import tachiyomi.core.common.preference.SourcePreferenceProfile
import tachiyomi.domain.library.update.model.LibraryUpdateProgress
import tachiyomi.domain.work.WorkOutcome

/**
 * The [LibraryUpdateWorkChain] as one chain of [InProcessWork]: each update is appended to the work named
 * [UNIQUE_NAME], and taking one out cancels only that one.
 */
@Inject
@SingleIn(AppScope::class)
class InProcessLibraryUpdateWorkChain(
    private val work: InProcessWork,
    private val runFactory: LibraryUpdateRun.Factory,
    // The queue reads this chain, and only a running update asks it who comes next.
    private val queue: Lazy<WorkChainLibraryUpdateQueue>,
) : LibraryUpdateWorkChain, ResumableWork {
    private val progress = MutableStateFlow<Map<String, LibraryUpdateProgress>>(emptyMap())

    override val kinds: Set<String> = setOf(UNIQUE_NAME)

    override val updates: Flow<List<ChainedLibraryUpdate>> =
        combine(work.works(UNIQUE_NAME), progress) { works, progress -> works.map { it.toChained(progress) } }

    override suspend fun currentUpdates(): List<ChainedLibraryUpdate> {
        val progress = progress.value
        return work.currentWorks(UNIQUE_NAME).map { it.toChained(progress) }
    }

    override suspend fun append(input: LibraryUpdateInput) {
        work.enqueue(
            UNIQUE_NAME,
            ExistingWorkPolicy.Append,
            InProcessWorkRequest(resumeAs = UNIQUE_NAME, input = input.toMap()) { run ->
                try {
                    // Extensions read logins and API keys from this profile's source preferences, not the active one's.
                    withContext(SourcePreferenceProfile.of(input.profileId)) {
                        runFactory.create(input) { published -> progress.update { it + (run.id to published) } }
                            .run(nextInQueue = queue.value.nextProfileFlow(run.id), onScoped = {})
                    }
                } finally {
                    progress.update { it - run.id }
                }
                WorkOutcome.Done
            },
        )
    }

    override suspend fun remove(update: ChainedLibraryUpdate) {
        work.cancelById(update.id)
    }

    override suspend fun resume(kind: String, input: Map<String, String>) {
        input.toLibraryUpdateInput()?.let { append(it) }
    }

    private fun InProcessWorkInfo.toChained(progress: Map<String, LibraryUpdateProgress>) = ChainedLibraryUpdate(
        id = id,
        profileId = input[KEY_PROFILE]?.toLongOrNull(),
        queuedAt = queuedAt,
        isRunning = isRunning,
        progress = progress[id],
    )

    private fun LibraryUpdateInput.toMap(): Map<String, String> = buildMap {
        put(KEY_PROFILE, profileId.toString())
        if (automatic) put(KEY_AUTOMATIC, true.toString())
        categoryId?.let { put(KEY_CATEGORY, it.toString()) }
        sourceId?.let { put(KEY_SOURCE, it.toString()) }
        entryType?.let { put(KEY_ENTRY_TYPE, it.name) }
        selectionName?.let { put(KEY_SELECTION, it) }
        if (skippedOfLatest) put(KEY_SKIPPED_OF_LATEST, true.toString())
    }

    private fun Map<String, String>.toLibraryUpdateInput(): LibraryUpdateInput? {
        return LibraryUpdateInput(
            profileId = get(KEY_PROFILE)?.toLongOrNull() ?: return null,
            automatic = get(KEY_AUTOMATIC).toBoolean(),
            categoryId = get(KEY_CATEGORY)?.toLongOrNull(),
            sourceId = get(KEY_SOURCE)?.toLongOrNull(),
            entryType = get(KEY_ENTRY_TYPE)?.let { name -> EntryType.entries.find { it.name == name } },
            selectionName = get(KEY_SELECTION),
            skippedOfLatest = get(KEY_SKIPPED_OF_LATEST).toBoolean(),
        )
    }

    private companion object {
        const val UNIQUE_NAME = "LibraryUpdate-queue"

        const val KEY_PROFILE = "profile"
        const val KEY_AUTOMATIC = "automatic"
        const val KEY_CATEGORY = "category"
        const val KEY_SOURCE = "source"
        const val KEY_ENTRY_TYPE = "entry_type"
        const val KEY_SELECTION = "selection"
        const val KEY_SKIPPED_OF_LATEST = "skipped_of_latest"
    }
}
