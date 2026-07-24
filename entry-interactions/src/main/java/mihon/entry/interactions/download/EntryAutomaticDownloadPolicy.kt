package mihon.entry.interactions

import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository

internal class EntryAutomaticDownloadPolicy(
    private val entryChapterRepository: EntryChapterRepository,
    private val downloadPreferences: DownloadPreferences,
    private val getCategories: GetCategories,
) {
    suspend fun evaluate(entry: Entry, newChapters: List<EntryChapter>): EntryAutomaticDownloadPolicyDecision {
        val hasNewChapters = newChapters.isNotEmpty()
        val enabled = downloadPreferences.downloadNewEntryChapters.get()
        val favorite = entry.favorite
        val categoryAllowed = hasNewChapters && enabled && favorite && entry.isAllowedByCategoryPolicy()
        val unreadOnly = downloadPreferences.downloadNewUnreadEntryChaptersOnly.get()
        val candidates = when {
            !categoryAllowed -> emptyList()
            !unreadOnly -> newChapters
            else -> newChapters.withoutPreviouslyConsumedNumbers(entry.id)
        }
        return EntryAutomaticDownloadPolicyDecision(
            candidates = candidates,
            hasNewChapters = hasNewChapters,
            enabled = enabled,
            favorite = favorite,
            categoryAllowed = categoryAllowed,
            unreadOnly = unreadOnly,
        )
    }

    private suspend fun Entry.isAllowedByCategoryPolicy(): Boolean {
        val categories = getCategories.await(id).map { it.id }.ifEmpty { listOf(DEFAULT_CATEGORY_ID) }
        val includedCategories = downloadPreferences.downloadNewEntryChapterCategories.get().map { it.toLong() }
        val excludedCategories = downloadPreferences.downloadNewEntryChapterCategoriesExclude.get().map { it.toLong() }

        return when {
            includedCategories.isEmpty() && excludedCategories.isEmpty() -> true
            categories.any { it in excludedCategories } -> false
            includedCategories.isEmpty() -> true
            else -> categories.any { it in includedCategories }
        }
    }

    private suspend fun List<EntryChapter>.withoutPreviouslyConsumedNumbers(entryId: Long): List<EntryChapter> {
        val consumedChapterNumbers = entryChapterRepository.getChaptersByEntryIdAwait(entryId)
            .asSequence()
            .filter { it.read && it.isRecognizedNumber }
            .map { it.chapterNumber }
            .toSet()
        return filterNot { it.chapterNumber in consumedChapterNumbers }
    }

    private companion object {
        const val DEFAULT_CATEGORY_ID = 0L
    }
}

internal data class EntryAutomaticDownloadPolicyDecision(
    val candidates: List<EntryChapter>,
    val hasNewChapters: Boolean,
    val enabled: Boolean,
    val favorite: Boolean,
    val categoryAllowed: Boolean,
    val unreadOnly: Boolean,
) {
    val blocker: EntryAutomaticDownloadBlocker? = when {
        !hasNewChapters -> EntryAutomaticDownloadBlocker.EMPTY_SELECTION
        !enabled -> EntryAutomaticDownloadBlocker.DISABLED
        !favorite -> EntryAutomaticDownloadBlocker.ENTRY_NOT_IN_LIBRARY
        !categoryAllowed -> EntryAutomaticDownloadBlocker.CATEGORY_POLICY_REJECTED
        unreadOnly && candidates.isEmpty() -> EntryAutomaticDownloadBlocker.NO_UNREAD_CANDIDATES
        else -> null
    }

    init {
        check((blocker == null) == candidates.isNotEmpty()) {
            "Automatic-download policy must select candidates exactly when its context is applicable"
        }
    }
}
