package mihon.feature.migration.list

import androidx.annotation.FloatRange
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import logcat.LogPriority
import mihon.entry.interactions.EntryCatalogueFeature
import mihon.entry.interactions.EntryCatalogueSourceInfo
import mihon.entry.interactions.EntryMigrationExecuteIntent
import mihon.entry.interactions.EntryMigrationExecutionResult
import mihon.entry.interactions.EntryMigrationFeature
import mihon.entry.interactions.EntryMigrationMode
import mihon.entry.interactions.EntryMigrationOption
import mihon.entry.interactions.EntryMigrationPreparationResult
import mihon.entry.interactions.EntryMigrationPrepareIntent
import mihon.entry.interactions.EntryMigrationSelectionResult
import mihon.entry.interactions.EntryMigrationSubject
import mihon.entry.interactions.EntryMigrationTargetRefreshIntent
import mihon.entry.interactions.EntryMigrationTargetRefreshResult
import mihon.feature.migration.list.models.MigratingEntry
import mihon.feature.migration.list.models.MigratingEntry.SearchResult
import mihon.feature.migration.list.search.SmartSourceSearchEngine
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrationListScreenModel(
    subjects: Collection<EntryMigrationSubject>,
    extraSearchQuery: String?,
    private val selectedOptions: Set<EntryMigrationOption>,
    private val preferences: SourcePreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val networkToLocalEntry: NetworkToLocalEntry = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val migration: EntryMigrationFeature = Injekt.get(),
    private val catalogueFeature: EntryCatalogueFeature = Injekt.get(),
) : StateScreenModel<MigrationListScreenModel.State>(State()) {

    private val smartSearchEngine = SmartSourceSearchEngine(extraSearchQuery, catalogueFeature)

    val items
        inline get() = state.value.items

    private val hideUnmatched = preferences.migrationHideUnmatched.get()
    private val hideWithoutUpdates = preferences.migrationHideWithoutUpdates.get()

    private val navigateBackChannel = Channel<Unit>()
    val navigateBackEvent = navigateBackChannel.receiveAsFlow()

    private val migrationFailureChannel = Channel<Unit>(Channel.BUFFERED)
    val migrationFailureEvent = migrationFailureChannel.receiveAsFlow()

    private var migrateJob: Job? = null

    init {
        screenModelScope.launchIO {
            val entries = subjects
                .map { subject ->
                    async {
                        val entry =
                            entryRepository.getEntryById(subject.entryId, subject.profileId) ?: return@async null
                        val chapterInfo = getChapterInfo(subject.entryId)
                        MigratingEntry(
                            subject = subject,
                            entry = entry,
                            chapterCount = chapterInfo.chapterCount,
                            latestChapter = chapterInfo.latestChapter,
                            source = sourceManager.getDisplayInfo(entry.source).name,
                            parentContext = screenModelScope.coroutineContext,
                        )
                    }
                }
                .awaitAll()
                .filterNotNull()
            when (migration.prepareSelection(entries.map(MigratingEntry::entry))) {
                is EntryMigrationSelectionResult.Ready -> {
                    mutableState.update { it.copy(items = entries) }
                    runMigrations(entries)
                }
                is EntryMigrationSelectionResult.Rejected -> {
                    migrationFailureChannel.send(Unit)
                    navigateBack()
                }
            }
        }
    }

    private suspend fun getChapterInfo(id: Long) = entryChapterRepository.getChaptersByEntryIdAwait(
        id,
    ).let { chapters ->
        ChapterInfo(
            latestChapter = chapters.maxOfOrNull { it.chapterNumber },
            chapterCount = chapters.size,
        )
    }

    private suspend fun Entry.toSuccessSearchResult(): SearchResult.Success {
        val chapterInfo = getChapterInfo(id)
        val source = sourceManager.getDisplayInfo(source).name
        return SearchResult.Success(
            entry = this,
            chapterCount = chapterInfo.chapterCount,
            latestChapter = chapterInfo.latestChapter,
            source = source,
        )
    }

    private suspend fun runMigrations(entries: List<MigratingEntry>) {
        val prioritizeByChapters = preferences.migrationPrioritizeByChapters.get()
        val deepSearchMode = preferences.migrationDeepSearchMode.get()

        val sourcesById = catalogueFeature.sources().associateBy { it.id }
        val sources = preferences.migrationSources.get().mapNotNull(sourcesById::get)

        for (entry in entries) {
            if (!currentCoroutineContext().isActive) break
            if (entry.entry.id !in state.value.entryIds) continue
            if (entry.searchResult.value != SearchResult.Searching) continue
            if (!entry.migrationScope.isActive) continue

            val result = try {
                entry.migrationScope.async {
                    if (prioritizeByChapters) {
                        val sourceSemaphore = Semaphore(5)
                        sources.map { source ->
                            async innerAsync@{
                                sourceSemaphore.withPermit {
                                    val result = searchSource(entry.entry, source, deepSearchMode)
                                    if (result == null || result.second.chapterCount == 0) return@innerAsync null
                                    result
                                }
                            }
                        }
                            .mapNotNull { it.await() }
                            .maxByOrNull { it.second.latestChapter ?: 0.0 }
                    } else {
                        sources.forEach { source ->
                            val result = searchSource(entry.entry, source, deepSearchMode)
                            if (result != null) return@async result
                        }
                        null
                    }
                }
                    .await()
            } catch (_: CancellationException) {
                continue
            }

            if (result != null && result.first.thumbnailUrl == null) {
                try {
                    migration.refreshTarget(
                        EntryMigrationTargetRefreshIntent(
                            source = entry.entry,
                            target = result.first,
                            fetchDetails = true,
                            fetchChildren = false,
                        ),
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }

            val resultEntry = result?.first?.id?.let {
                entryRepository.getEntryById(it, entry.entry.profileId)
            } ?: result?.first
            entry.searchResult.value = resultEntry?.toSuccessSearchResult() ?: SearchResult.NotFound

            if (result == null && hideUnmatched) {
                removeEntry(entry)
            }
            if (result != null &&
                hideWithoutUpdates &&
                (result.second.latestChapter ?: 0.0) <= (entry.latestChapter ?: 0.0)
            ) {
                removeEntry(entry)
            }

            updateMigrationProgress()
        }
    }

    private suspend fun searchSource(
        entry: Entry,
        source: EntryCatalogueSourceInfo,
        deepSearchMode: Boolean,
    ): Pair<Entry, ChapterInfo>? {
        return try {
            val searchResult = if (deepSearchMode) {
                smartSearchEngine.deepSearch(source, entry.title, entry.type)
            } else {
                smartSearchEngine.regularSearch(source, entry.title, entry.type)
            }

            if (searchResult == null || (searchResult.url == entry.url && source.id == entry.source)) return null

            val localEntry = networkToLocalEntry(searchResult)
            when (
                val refresh = migration.refreshTarget(
                    EntryMigrationTargetRefreshIntent(
                        source = entry,
                        target = localEntry,
                        fetchDetails = false,
                        fetchChildren = true,
                    ),
                )
            ) {
                is EntryMigrationTargetRefreshResult.OperationalFailure -> logcat(LogPriority.ERROR, refresh.error)
                EntryMigrationTargetRefreshResult.Refreshed,
                is EntryMigrationTargetRefreshResult.Rejected,
                EntryMigrationTargetRefreshResult.SourceUnavailable,
                EntryMigrationTargetRefreshResult.NoChildren,
                -> Unit
            }
            val preparation = migration.prepare(EntryMigrationPrepareIntent(entry, localEntry))
            if (preparation !is EntryMigrationPreparationResult.Ready) return null
            localEntry to getChapterInfo(localEntry.id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun updateMigrationProgress() {
        mutableState.update { state ->
            state.copy(
                finishedCount = items.count { it.searchResult.value != SearchResult.Searching },
                migrationComplete = migrationComplete(),
            )
        }
        if (items.isEmpty()) {
            navigateBack()
        }
    }

    private fun migrationComplete() = items.all { it.searchResult.value != SearchResult.Searching } &&
        items.any { it.searchResult.value is SearchResult.Success }

    fun useEntryForMigration(current: Long, target: Long, onMissingChapters: () -> Unit) {
        val migratingEntry = items.find { it.entry.id == current } ?: return
        migratingEntry.searchResult.value = SearchResult.Searching
        screenModelScope.launchIO {
            val result = migratingEntry.migrationScope.async {
                val entry = entryRepository.getEntryById(target, migratingEntry.entry.profileId) ?: return@async null
                try {
                    val refresh = migration.refreshTarget(
                        EntryMigrationTargetRefreshIntent(
                            source = migratingEntry.entry,
                            target = entry,
                            fetchDetails = false,
                            fetchChildren = true,
                        ),
                    )
                    if (refresh != EntryMigrationTargetRefreshResult.Refreshed) return@async null
                    entryRepository.getEntryById(target, migratingEntry.entry.profileId) ?: entry
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            }
                .await()

            if (result == null) {
                migratingEntry.searchResult.value = SearchResult.NotFound
                withUIContext { onMissingChapters() }
                return@launchIO
            }

            val preparation = migration.prepare(EntryMigrationPrepareIntent(migratingEntry.entry, result))
            if (preparation !is EntryMigrationPreparationResult.Ready) {
                migratingEntry.searchResult.value = SearchResult.NotFound
                migrationFailureChannel.send(Unit)
                return@launchIO
            }

            migratingEntry.searchResult.value = result.toSuccessSearchResult()
            updateMigrationProgress()
        }
    }

    fun migrateEntries() {
        migrateEntries(replace = true)
    }

    fun copyEntries() {
        migrateEntries(replace = false)
    }

    private fun migrateEntries(replace: Boolean) {
        migrateJob = screenModelScope.launchIO {
            mutableState.update { it.copy(dialog = Dialog.Progress(0f)) }
            val items = items
            var failed = false
            try {
                items.forEachIndexed { index, entry ->
                    try {
                        ensureActive()
                        val target = entry.searchResult.value.let {
                            if (it is SearchResult.Success) {
                                it.entry
                            } else {
                                null
                            }
                        }
                        if (target != null) {
                            failed = !executeMigration(entry.entry, target, replace, selectedOptions) || failed
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        failed = true
                        logcat(LogPriority.WARN, throwable = e)
                    }
                    mutableState.update {
                        it.copy(dialog = Dialog.Progress(((index + 1).toFloat() / items.size).coerceAtMost(1f)))
                    }
                }

                if (failed) {
                    migrationFailureChannel.send(Unit)
                }
                navigateBack()
            } finally {
                mutableState.update { it.copy(dialog = null) }
                migrateJob = null
            }
        }
    }

    fun cancelMigrate() {
        migrateJob?.cancel()
        migrateJob = null
    }

    private suspend fun navigateBack() {
        navigateBackChannel.send(Unit)
    }

    fun migrateNow(entryId: Long, replace: Boolean) {
        screenModelScope.launchIO {
            val entry = items.find { it.entry.id == entryId } ?: return@launchIO
            val target = (entry.searchResult.value as? SearchResult.Success)?.entry ?: return@launchIO
            if (executeMigration(entry.entry, target, replace, selectedOptions)) {
                removeEntry(entryId)
            } else {
                migrationFailureChannel.send(Unit)
            }
        }
    }

    private suspend fun executeMigration(
        source: Entry,
        target: Entry,
        replace: Boolean,
        selectedOptions: Set<EntryMigrationOption>,
    ): Boolean {
        val preparation = migration.prepare(EntryMigrationPrepareIntent(source, target))
        if (preparation !is EntryMigrationPreparationResult.Ready) return false
        val result = migration.execute(
            EntryMigrationExecuteIntent(
                reference = preparation.reference,
                mode = if (replace) EntryMigrationMode.REPLACE else EntryMigrationMode.COPY,
                selectedOptions = selectedOptions.intersect(preparation.availableOptions),
            ),
        )
        return result is EntryMigrationExecutionResult.Applied
    }

    fun removeEntry(entryId: Long) {
        screenModelScope.launchIO {
            val item = items.find { it.entry.id == entryId } ?: return@launchIO
            removeEntry(item)
            item.migrationScope.cancel()
            updateMigrationProgress()
        }
    }

    private fun removeEntry(item: MigratingEntry) {
        mutableState.update { it.copy(items = items.toMutableList().apply { remove(item) }) }
    }

    override fun onDispose() {
        super.onDispose()
        items.forEach {
            it.migrationScope.cancel()
        }
    }

    fun showMigrateDialog(copy: Boolean) {
        mutableState.update { state ->
            state.copy(
                dialog = Dialog.Migrate(
                    copy = copy,
                    totalCount = items.size,
                    skippedCount = items.count { it.searchResult.value == SearchResult.NotFound },
                ),
            )
        }
    }

    fun showExitDialog() {
        mutableState.update {
            it.copy(dialog = Dialog.Exit)
        }
    }

    fun dismissDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    data class ChapterInfo(
        val latestChapter: Double?,
        val chapterCount: Int,
    )

    sealed interface Dialog {
        data class Migrate(val copy: Boolean, val totalCount: Int, val skippedCount: Int) : Dialog
        data class Progress(@FloatRange(0.0, 1.0) val progress: Float) : Dialog
        data object Exit : Dialog
    }

    data class State(
        val items: List<MigratingEntry> = listOf(),
        val finishedCount: Int = 0,
        val migrationComplete: Boolean = false,
        val dialog: Dialog? = null,
    ) {
        val entryIds: List<Long> = items.map { it.entry.id }
    }
}
