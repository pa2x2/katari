package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import java.util.concurrent.atomic.AtomicBoolean

internal interface EntryAutomaticDownloadCoordinator : EntryAutomaticDownloadFeature {
    fun newLibraryUpdateBatch(): EntryAutomaticDownloadBatch

    suspend fun downloadAfterEntryRefresh(
        entry: Entry,
        newChapters: List<EntryChapter>,
    ): EntryAutomaticDownloadResult
}

internal interface EntryAutomaticDownloadBatch {
    suspend fun enqueue(
        entry: Entry,
        newChapters: List<EntryChapter>,
    ): EntryAutomaticDownloadResult

    fun complete()
}

internal class DefaultEntryAutomaticDownloadFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val interaction: EntryDownloadInteraction,
    private val sharedPolicy: EntryAutomaticDownloadPolicy,
) : EntryAutomaticDownloadCoordinator {
    private val applicableTypes = evaluation.automaticDownloadTypes()

    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override fun newLibraryUpdateBatch(): EntryAutomaticDownloadBatch {
        return DefaultEntryAutomaticDownloadBatch(::scheduleForLibraryUpdate, interaction::startDownloads)
    }

    override suspend fun downloadAfterEntryRefresh(
        entry: Entry,
        newChapters: List<EntryChapter>,
    ): EntryAutomaticDownloadResult {
        return when (val candidates = selectCandidates(entry, newChapters)) {
            is SelectedCandidates.Empty -> candidates.result
            is SelectedCandidates.Selected -> {
                interaction.download(entry, candidates.chapters)
                EntryAutomaticDownloadResult.Scheduled(candidates.chapters.size)
            }
        }
    }

    private suspend fun scheduleForLibraryUpdate(
        entry: Entry,
        newChapters: List<EntryChapter>,
    ): EntryAutomaticDownloadResult {
        return when (val candidates = selectCandidates(entry, newChapters)) {
            is SelectedCandidates.Empty -> candidates.result
            is SelectedCandidates.Selected -> {
                interaction.queue(entry, candidates.chapters, autoStart = false)
                EntryAutomaticDownloadResult.Scheduled(candidates.chapters.size)
            }
        }
    }

    private suspend fun selectCandidates(
        entry: Entry,
        newChapters: List<EntryChapter>,
    ): SelectedCandidates {
        if (!isApplicable(entry.type)) {
            return SelectedCandidates.Empty(EntryAutomaticDownloadResult.Inapplicable(entry.type))
        }

        val decision = sharedPolicy.evaluate(entry, newChapters)
        evaluation.requireAutomaticDownloadContext(entry.type, decision)
        val blocker = decision.blocker
        return if (blocker != null) {
            SelectedCandidates.Empty(EntryAutomaticDownloadResult.Blocked(setOf(blocker)))
        } else {
            SelectedCandidates.Selected(decision.candidates)
        }
    }

    private sealed interface SelectedCandidates {
        data class Empty(val result: EntryAutomaticDownloadResult) : SelectedCandidates

        data class Selected(val chapters: List<EntryChapter>) : SelectedCandidates
    }
}

private class DefaultEntryAutomaticDownloadBatch(
    private val schedule: suspend (Entry, List<EntryChapter>) -> EntryAutomaticDownloadResult,
    private val startDownloads: () -> Unit,
) : EntryAutomaticDownloadBatch {
    private val queuedWork = AtomicBoolean(false)
    private val completed = AtomicBoolean(false)

    override suspend fun enqueue(
        entry: Entry,
        newChapters: List<EntryChapter>,
    ): EntryAutomaticDownloadResult {
        check(!completed.get()) { "Cannot enqueue automatic downloads after the library-update batch completed" }
        return schedule(entry, newChapters).also { result ->
            if (result is EntryAutomaticDownloadResult.Scheduled) queuedWork.set(true)
        }
    }

    override fun complete() {
        if (completed.compareAndSet(false, true) && queuedWork.get()) {
            startDownloads()
        }
    }
}
