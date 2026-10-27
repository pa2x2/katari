package mihon.feature.migration.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mihon.app.di.AppUiScope
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.migration.session.model.SourceMigrationDiscoveryDepth
import mihon.feature.migration.session.model.SourceMigrationGroupSelection
import mihon.feature.migration.session.model.SourceMigrationSessionDraft
import mihon.feature.migration.session.model.SourceMigrationSessionGroupDraft
import mihon.feature.migration.session.model.SourceMigrationSessionId
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.source.model.EntryCatalogueDescription
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceManager

@AssistedInject
internal class MigrationConfigViewModel(
    @Assisted private val sessionGroups: List<SourceMigrationGroupSelection>,
    @Assisted private val excludedSourceId: Long?,
    @Assisted private val persistSourceSelection: Boolean,
    val sourcePreferences: SourcePreferences,
    private val sourceManager: SourceManager,
    private val catalogueFeature: EntryCatalogueFeature,
    private val sessionStore: SourceMigrationSessionStore,
    private val getEntry: GetEntry,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(
            sessionGroups: List<SourceMigrationGroupSelection>,
            excludedSourceId: Long?,
            persistSourceSelection: Boolean,
        ): MigrationConfigViewModel
    }

    val state: StateFlow<MigrationConfigViewModel.State>
        field = MutableStateFlow<MigrationConfigViewModel.State>(State())

    private val sourcesComparator = { includedSources: List<Long> ->
        compareBy<MigrationSource>(
            { !it.isSelected },
            { includedSources.indexOf(it.id) },
            { with(it) { "$name ($shortLanguage)" } },
        )
    }

    private var sessionGroupDrafts: List<SourceMigrationSessionGroupDraft> = emptyList()

    init {
        viewModelScope.launchIO {
            sessionGroupDrafts = loadSessionGroups()
            initSources(
                requiredEntryTypes = sessionGroupDrafts.flatMapTo(mutableSetOf()) { group ->
                    group.members.filter { it.id in group.selectedEntryIds }.map { it.type }
                },
            )
            state.update { it.copy(isLoading = false) }
        }
    }

    private fun updateSources(action: (List<MigrationSource>) -> List<MigrationSource>) {
        state.update { state ->
            val updatedSources = action(state.sources)
            val includedSources = updatedSources.mapNotNull { if (!it.isSelected) null else it.id }
            state.copy(sources = updatedSources.sortedWith(sourcesComparator(includedSources)))
        }
        if (persistSourceSelection) saveSources()
    }

    private suspend fun loadSessionGroups(): List<SourceMigrationSessionGroupDraft> {
        val entriesById = getEntry.await(sessionGroups.flatMap { it.memberIds }).associateBy { it.id }
        // An Entry removed since it was selected drops out; a group left without a selected member drops entirely
        return sessionGroups.mapNotNull { selection ->
            val members = selection.memberIds.mapNotNull(entriesById::get)
            val visibleEntry = entriesById[selection.visibleEntryId] ?: return@mapNotNull null
            val selectedEntryIds = selection.selectedEntryIds.filterTo(mutableSetOf()) { it in entriesById }
            if (selectedEntryIds.isEmpty()) return@mapNotNull null
            SourceMigrationSessionGroupDraft(visibleEntry, members, selectedEntryIds)
        }
    }

    private fun initSources(requiredEntryTypes: Set<EntryType>) {
        val languages = sourcePreferences.enabledLanguages.get()
        val pinnedSources = sourcePreferences.pinnedSources.get().mapNotNull { it.toLongOrNull() }
        val includedSources = sourcePreferences.migrationSources.get()
        val disabledSources = sourcePreferences.disabledSources.get()
            .mapNotNull { it.toLongOrNull() }
        val sources = catalogueFeature.sources()
            .asSequence()
            .mapNotNull {
                if (it.language !in languages) return@mapNotNull null
                if (it.id == excludedSourceId) return@mapNotNull null
                val supportedEntryTypes = it.supportedEntryTypes
                if (supportedEntryTypes != null && !supportedEntryTypes.containsAll(requiredEntryTypes)) {
                    return@mapNotNull null
                }
                val source = Source(
                    id = it.id,
                    lang = it.language,
                    name = it.name,
                    catalogue = EntryCatalogueDescription(it.supportsLatest),
                    isStub = false,
                )
                MigrationSource(
                    source = source,
                    isSelected = when {
                        includedSources.isNotEmpty() -> source.id in includedSources
                        pinnedSources.isNotEmpty() -> source.id in pinnedSources
                        else -> source.id !in disabledSources
                    },
                )
            }
            .toList()

        state.update { state ->
            state.copy(sources = sources.sortedWith(sourcesComparator(includedSources)))
        }
    }

    fun toggleSelection(id: Long) {
        updateSources { sources ->
            sources.map { source ->
                source.copy(isSelected = if (source.source.id == id) !source.isSelected else source.isSelected)
            }
        }
    }

    fun toggleSelection(config: SelectionConfig) {
        val pinnedSources = sourcePreferences.pinnedSources.get().mapNotNull { it.toLongOrNull() }
        val disabledSources = sourcePreferences.disabledSources.get().mapNotNull { it.toLongOrNull() }
        val isSelected: (Long) -> Boolean = {
            when (config) {
                SelectionConfig.All -> true
                SelectionConfig.None -> false
                SelectionConfig.Pinned -> it in pinnedSources
                SelectionConfig.Enabled -> it !in disabledSources
            }
        }
        updateSources { sources ->
            sources.map { source ->
                source.copy(isSelected = isSelected(source.source.id))
            }
        }
    }

    fun orderSource(from: Int, to: Int) {
        updateSources {
            it.toMutableList()
                .apply {
                    add(to, removeAt(from))
                }
                .toList()
        }
    }

    fun saveSources() {
        if (!persistSourceSelection) return
        state.value.sources
            .filter { source -> source.isSelected }
            .map { source -> source.source.id }
            .let { sources -> sourcePreferences.migrationSources.set(sources) }
    }

    fun startSession(
        originSourceId: Long,
        selectedOptions: Set<EntryMigrationOption>,
        initialDiscoveryDepth: SourceMigrationDiscoveryDepth,
    ) {
        if (state.value.isStarting) return
        val groups = sessionGroupDrafts
        val profileId = groups.firstOrNull()?.members?.first()?.profileId ?: return
        val targetSourceIds = state.value.selectedSourceIds
        if (targetSourceIds.isEmpty()) return
        state.update { it.copy(isStarting = true) }
        viewModelScope.launchIO {
            val sessionId = sessionStore.create(
                SourceMigrationSessionDraft(
                    profileId = profileId,
                    originSourceId = originSourceId,
                    groups = groups,
                    targetSourceIds = targetSourceIds,
                    selectedOptions = selectedOptions,
                    initialDiscoveryDepth = initialDiscoveryDepth,
                ),
            )
            state.update { it.copy(createdSessionId = sessionId) }
        }
    }

    data class State(
        val isLoading: Boolean = true,
        val sources: List<MigrationSource> = emptyList(),
        val isStarting: Boolean = false,
        val createdSessionId: SourceMigrationSessionId? = null,
    ) {
        val selectedSourceIds: List<Long>
            get() = sources.filter(MigrationSource::isSelected).map(MigrationSource::id)
    }

    enum class SelectionConfig {
        All,
        None,
        Pinned,
        Enabled,
    }
}

data class MigrationSource(
    val source: Source,
    val isSelected: Boolean,
) {
    val id: Long
        inline get() = source.id

    val name: String
        inline get() = source.name

    val shortLanguage: String = LocaleHelper.getShortDisplayName(source.lang)
}
