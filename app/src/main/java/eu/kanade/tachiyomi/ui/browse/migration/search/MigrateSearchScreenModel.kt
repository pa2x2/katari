package eu.kanade.tachiyomi.ui.browse.migration.search

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateSearchScreenModel(
    val entryId: Long,
    getEntry: GetEntry = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
) : SearchScreenModel() {

    private val migrationSources by lazy { sourcePreferences.migrationSources.get() }
    private var entryType: eu.kanade.tachiyomi.source.entry.EntryType? = null

    override val sortComparator = { map: Map<UnifiedSource, SearchItemResult> ->
        compareBy<UnifiedSource>(
            { (map[it] as? SearchItemResult.Success)?.isEmpty ?: true },
            { migrationSources.indexOf(it.id) },
        )
    }

    init {
        screenModelScope.launch {
            val entry = getEntry.await(entryId)!!
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

    override fun getEnabledSources(): List<UnifiedSource> {
        return migrationSources.mapNotNull { sourceManager.getCatalogueSource(it) }
    }

    override fun filterSearchResults(entries: List<Entry>): List<Entry> {
        val type = entryType ?: return emptyList()
        return entries.filter { it.type == type }
    }
}
