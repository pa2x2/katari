package eu.kanade.tachiyomi.ui.browse.migration.search

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.entry.interactions.EntryCatalogueFeature
import mihon.entry.interactions.EntryCatalogueSourceInfo
import mihon.entry.interactions.EntryMigrationFeature
import mihon.entry.interactions.EntryMigrationPreparationResult
import mihon.entry.interactions.EntryMigrationPrepareIntent
import mihon.entry.interactions.EntryMigrationSubject
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateSearchScreenModel(
    val subject: EntryMigrationSubject,
    private val entryRepository: EntryRepository = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val migration: EntryMigrationFeature = Injekt.get(),
    private val catalogueFeature: EntryCatalogueFeature = Injekt.get(),
) : SearchScreenModel() {

    private val migrationSources by lazy { sourcePreferences.migrationSources.get() }
    private var entryType: eu.kanade.tachiyomi.source.entry.EntryType? = null

    override val sortComparator = { map: Map<EntryCatalogueSourceInfo, SearchItemResult> ->
        compareBy<EntryCatalogueSourceInfo>(
            { (map[it] as? SearchItemResult.Success)?.isEmpty ?: true },
            { migrationSources.indexOf(it.id) },
        )
    }

    init {
        screenModelScope.launch {
            val entry = entryRepository.getEntryById(subject.entryId, subject.profileId) ?: return@launch
            entryType = entry.type
            mutableState.update {
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
