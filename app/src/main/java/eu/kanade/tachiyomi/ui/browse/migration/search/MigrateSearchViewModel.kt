package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchViewModel
import kotlinx.coroutines.launch
import mihon.app.di.AppUiScope
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.catalogue.EntryCatalogueSourceInfo
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationPreparationResult
import mihon.entry.interactions.migration.EntryMigrationPrepareIntent
import mihon.entry.interactions.migration.EntryMigrationSubject
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

@AssistedInject
class MigrateSearchViewModel(
    @Assisted val subject: EntryMigrationSubject,
    private val entryRepository: EntryRepository,
    private val sourcePreferences: SourcePreferences,
    private val migration: EntryMigrationFeature,
    private val catalogueFeature: EntryCatalogueFeature,
    extensionManager: ExtensionManager,
    networkToLocalEntry: NetworkToLocalEntry,
    getEntry: GetEntry,
) : SearchViewModel(
    extensionManager = extensionManager,
    networkToLocalEntry = networkToLocalEntry,
    getEntry = getEntry,
    preferences = sourcePreferences,
    catalogueFeature = catalogueFeature,
) {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(subject: EntryMigrationSubject): MigrateSearchViewModel
    }

    private val migrationSources by lazy { sourcePreferences.migrationSources.get() }
    private var entryType: eu.kanade.tachiyomi.source.entry.EntryType? = null

    override val sortComparator = { map: Map<EntryCatalogueSourceInfo, SearchItemResult> ->
        compareBy<EntryCatalogueSourceInfo>(
            { (map[it] as? SearchItemResult.Success)?.isEmpty ?: true },
            { migrationSources.indexOf(it.id) },
        )
    }

    init {
        viewModelScope.launch {
            val entry = entryRepository.getEntryById(subject.entryId, subject.profileId) ?: return@launch
            entryType = entry.type
            updateState {
                it.copy(
                    from = entry,
                    searchQuery = entry.title,
                )
            }
            search()
        }
    }

    override fun getEnabledSources(): List<EntryCatalogueSourceInfo> {
        val sourcesById = catalogueFeature.sources().associateBy { it.id }
        return migrationSources.mapNotNull(sourcesById::get)
    }

    override fun filterSearchResults(entries: List<Entry>): List<Entry> {
        val type = entryType ?: return emptyList()
        return entries.filter { it.type == type }
    }

    suspend fun acceptsTarget(target: Entry): Boolean {
        val source = state.value.from ?: return false
        return migration.prepare(EntryMigrationPrepareIntent(source, target)) is EntryMigrationPreparationResult.Ready
    }
}
