package eu.kanade.tachiyomi.ui.browse.extension

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.extension.interactor.GetExtensionLanguages
import eu.kanade.domain.extension.interactor.GetExtensionsByType
import eu.kanade.domain.source.interactor.ToggleLanguage
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.browse.BrowseContentTypeFilterController
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.app.di.AppUiScope
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.source.EntrySourceHomeFeature
import mihon.entry.interactions.source.EntrySourceHomeResolution
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class ExtensionsViewModel(
    basePreferences: BasePreferences,
    private val context: Application,
    private val extensionManager: ExtensionManager,
    private val getExtensions: GetExtensionsByType,
    private val preferences: SourcePreferences,
    private val getExtensionLanguages: GetExtensionLanguages,
    private val toggleLanguage: ToggleLanguage,
    private val sourceHomeFeature: EntrySourceHomeFeature,
    private val catalogueFeature: EntryCatalogueFeature,
    private val contentTypeFilterController: BrowseContentTypeFilterController,
    private val storeFilterController: ExtensionStoreFilterController,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow<String?>(null)

    // Separate from [state] so BrowseTab's search bar and the tab's badges don't subscribe to the extension list
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()

    val updatesCount: StateFlow<Int> = combine(
        extensionManager.pendingUpdatesCount,
        extensionManager.isAutoUpdateInProgress,
    ) { updates, inProgress ->
        if (inProgress) 0 else updates
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), 0)

    val filter: StateFlow<ExtensionFilterState> = combine(
        getExtensionLanguages.subscribe(),
        preferences.enabledLanguages.changes(),
        contentTypeFilterController.changes(),
        storeFilterController.changes(),
    ) { languages, enabledLanguages, contentTypes, stores ->
        ExtensionFilterState(
            languages = languages,
            enabledLanguages = enabledLanguages,
            contentTypes = contentTypes,
            stores = stores,
        )
    }
        .catch { throwable -> logcat(LogPriority.ERROR, throwable) }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), ExtensionFilterState())

    private val isRefreshing = MutableStateFlow(false)

    private fun extensionMapper(map: Map<String, InstallStep>): (Extension) -> ExtensionUiModel.Item = {
        ExtensionUiModel.Item(it, map[it.pkgName] ?: InstallStep.Idle)
    }

    private val items = combine(
        _searchQuery
            .debounce(0.25.seconds)
            .map { searchQueryPredicate(it ?: "") },
        contentTypeFilterController.changes(),
        storeFilterController.changes(),
        extensionManager.installSteps(),
        getExtensions.subscribe(),
    ) { searchPredicate, contentTypeFilter, storeFilter, downloads, extensions ->
        val (_updates, _loaded, _available, _notLoaded) = extensions
        val predicate: (Extension) -> Boolean = {
            searchPredicate(it) && contentTypeFilter.matches(it, catalogueFeature) && storeFilter.matches(it)
        }
        buildMap {
            val updates = _updates.filter(predicate).map(extensionMapper(downloads))
            if (updates.isNotEmpty()) {
                put(ExtensionUiModel.Header.Resource(MR.strings.ext_updates_pending), updates)
            }

            val notLoaded = _notLoaded.filter(predicate).map(extensionMapper(downloads))
            if (notLoaded.isNotEmpty()) {
                put(ExtensionUiModel.Header.Resource(MR.strings.ext_not_loaded), notLoaded)
            }

            val loaded = _loaded.filter(predicate).map(extensionMapper(downloads))
            if (loaded.isNotEmpty()) {
                put(ExtensionUiModel.Header.Resource(MR.strings.ext_installed), loaded)
            }

            val languagesWithExtensions = _available
                .filter(predicate)
                .groupBy { it.lang }
                .toSortedMap(LocaleHelper.comparator)
                .map { (lang, exts) ->
                    ExtensionUiModel.Header.Text(LocaleHelper.getSourceDisplayName(lang, context)) to
                        exts.map(extensionMapper(downloads))
                }
            if (languagesWithExtensions.isNotEmpty()) {
                putAll(languagesWithExtensions)
            }
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)

    val state: StateFlow<ExtensionListState> = combine(
        items,
        _searchQuery,
        filter,
        combine(isRefreshing, updatesCount, basePreferences.extensionInstaller.changes(), ::Triple),
    ) { items, searchQuery, filter, (isRefreshing, updates, installer) ->
        ExtensionListState(
            isLoading = items == null,
            isRefreshing = isRefreshing,
            items = items.orEmpty(),
            updates = updates,
            installer = installer,
            searchQuery = searchQuery,
            filter = filter,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), ExtensionListState())

    init {
        viewModelScope.launchIO { findAvailableExtensions() }
    }

    fun searchQueryPredicate(query: String): (Extension) -> Boolean {
        val subqueries = query.split(",")
            .map { it.trim() }
            .filterNot { it.isBlank() }

        if (subqueries.isEmpty()) return { true }

        return { extension ->
            subqueries.any { subquery ->
                if (extension.name.contains(subquery, ignoreCase = true)) return@any true

                when (extension) {
                    is Extension.Loaded -> extension.sources.any { source ->
                        source.name.contains(subquery, ignoreCase = true) ||
                            (
                                sourceHomeFeature.resolve(
                                    source.id,
                                ) as? EntrySourceHomeResolution.Available
                                )?.url?.contains(
                                subquery,
                                ignoreCase = true,
                            ) ==
                            true ||
                            source.id == subquery.toLongOrNull()
                    }

                    is Extension.Available -> extension.sources.any {
                        it.name.contains(subquery, ignoreCase = true) ||
                            it.baseUrl.contains(subquery, ignoreCase = true) ||
                            it.id == subquery.toLongOrNull()
                    }

                    else -> false
                }
            }
        }
    }

    fun search(query: String?) {
        _searchQuery.update { query }
    }

    fun toggleContentType(entryType: EntryType) {
        contentTypeFilterController.toggle(entryType)
    }

    fun toggleUnspecifiedContentType() {
        contentTypeFilterController.toggleUnspecified()
    }

    fun showAllContentTypes() {
        contentTypeFilterController.showAll()
    }

    fun toggleStore(storeUrl: String) {
        storeFilterController.toggle(storeUrl)
    }

    fun toggleLanguage(language: String) {
        toggleLanguage.await(language)
    }

    fun resetFilters() {
        showAllContentTypes()
        storeFilterController.showAll()
        preferences.enabledLanguages.getAndSet { enabledLanguages ->
            enabledLanguages + filter.value.languages
        }
    }

    fun updateAllExtensions() {
        viewModelScope.launchIO {
            (extensionManager.getLoadedExtensions() + extensionManager.getNotLoadedExtensions())
                .filter { it.hasUpdate }
                .forEach(::updateExtension)
        }
    }

    fun installExtension(extension: Extension.Available) {
        viewModelScope.launchIO {
            extensionManager.installExtension(extension).collectToInstallUpdate(extension)
        }
    }

    fun updateExtension(extension: Extension.Installed) {
        viewModelScope.launchIO {
            extensionManager.updateExtension(extension).collectToInstallUpdate(extension)
        }
    }

    fun reinstallExtension(extension: Extension.Installed) {
        viewModelScope.launchIO {
            extensionManager.reinstallExtension(extension).collectToInstallUpdate(extension)
        }
    }

    fun cancelInstallUpdateExtension(extension: Extension) {
        extensionManager.cancelInstallUpdateExtension(extension)
    }

    private suspend fun Flow<InstallStep>.collectToInstallUpdate(extension: Extension) =
        this
            .firstOrNull { it.isCompleted() }

    fun uninstallExtension(extension: Extension.Installed) {
        extensionManager.uninstallExtension(extension)
    }

    fun findAvailableExtensions() {
        viewModelScope.launchIO {
            isRefreshing.update { true }

            extensionManager.findAvailableExtensions(forceRefresh = true)

            // Fake slower refresh so it doesn't seem like it's not doing anything
            delay(1.seconds)

            isRefreshing.update { false }
        }
    }

    fun trustExtension(extension: Extension.NotLoaded) {
        extensionManager.trust(extension)
    }
}
