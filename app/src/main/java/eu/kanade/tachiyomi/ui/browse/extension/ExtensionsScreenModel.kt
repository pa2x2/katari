package eu.kanade.tachiyomi.ui.browse.extension

import android.app.Application
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.EntrySourceHomeFeature
import mihon.entry.interactions.EntrySourceHomeResolution
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

class ExtensionsScreenModel(
    basePreferences: BasePreferences = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val getExtensions: GetExtensionsByType = Injekt.get(),
    private val preferences: SourcePreferences = Injekt.get(),
    private val getExtensionLanguages: GetExtensionLanguages = Injekt.get(),
    private val toggleLanguage: ToggleLanguage = Injekt.get(),
    private val sourceHomeFeature: EntrySourceHomeFeature = Injekt.get(),
    private val contentTypeFilterController: BrowseContentTypeFilterController =
        BrowseContentTypeFilterController(preferences),
) : StateScreenModel<ExtensionListState>(ExtensionListState()) {

    init {
        val context = Injekt.get<Application>()
        val extensionMapper: (Map<String, InstallStep>) -> ((Extension) -> ExtensionUiModel.Item) = { map ->
            {
                ExtensionUiModel.Item(it, map[it.pkgName] ?: InstallStep.Idle)
            }
        }

        screenModelScope.launchIO {
            combine(
                state.map { it.searchQuery }
                    .distinctUntilChanged()
                    .debounce(0.25.seconds)
                    .map { searchQueryPredicate(it ?: "") },
                contentTypeFilterController.changes(),
                extensionManager.installSteps(),
                getExtensions.subscribe(),
            ) { searchPredicate, contentTypeFilter, downloads, extensions ->
                val (_updates, _installed, _available, _untrusted) = extensions
                val predicate: (Extension) -> Boolean = {
                    searchPredicate(it) && contentTypeFilter.matches(it)
                }
                buildMap {
                    val updates = _updates.filter(predicate).map(extensionMapper(downloads))
                    if (updates.isNotEmpty()) {
                        put(ExtensionUiModel.Header.Resource(MR.strings.ext_updates_pending), updates)
                    }

                    val installed = _installed.filter(predicate).map(extensionMapper(downloads))
                    val untrusted = _untrusted.filter(predicate).map(extensionMapper(downloads))
                    if (installed.isNotEmpty() || untrusted.isNotEmpty()) {
                        put(ExtensionUiModel.Header.Resource(MR.strings.ext_installed), installed + untrusted)
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
                .collectLatest { items ->
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            items = items,
                        )
                    }
                }
        }

        screenModelScope.launchIO {
            combine(
                getExtensionLanguages.subscribe(),
                preferences.enabledLanguages.changes(),
                contentTypeFilterController.changes(),
            ) { languages, enabledLanguages, contentTypes ->
                ExtensionFilterState(
                    languages = languages,
                    enabledLanguages = enabledLanguages,
                    contentTypes = contentTypes,
                )
            }
                .catch { throwable -> logcat(LogPriority.ERROR, throwable) }
                .collectLatest { filter ->
                    mutableState.update { it.copy(filter = filter) }
                }
        }

        screenModelScope.launchIO { findAvailableExtensions() }

        combine(
            extensionManager.pendingUpdatesCount,
            extensionManager.isAutoUpdateInProgress,
        ) { updates, inProgress ->
            if (inProgress) 0 else updates
        }
            .onEach { mutableState.update { state -> state.copy(updates = it) } }
            .launchIn(screenModelScope)

        basePreferences.extensionInstaller.changes()
            .onEach { mutableState.update { state -> state.copy(installer = it) } }
            .launchIn(screenModelScope)
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
                    is Extension.Installed -> extension.sources.any { source ->
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
        mutableState.update {
            it.copy(searchQuery = query)
        }
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

    fun toggleLanguage(language: String) {
        toggleLanguage.await(language)
    }

    fun resetFilters() {
        showAllContentTypes()
        preferences.enabledLanguages.getAndSet { enabledLanguages ->
            enabledLanguages + state.value.filter.languages
        }
    }

    fun updateAllExtensions() {
        screenModelScope.launchIO {
            extensionManager.installedExtensionsFlow.value
                .filter { it.hasUpdate }
                .forEach(::updateExtension)
        }
    }

    fun installExtension(extension: Extension.Available) {
        screenModelScope.launchIO {
            extensionManager.installExtension(extension).collectToInstallUpdate(extension)
        }
    }

    fun updateExtension(extension: Extension.Installed) {
        screenModelScope.launchIO {
            extensionManager.updateExtension(extension).collectToInstallUpdate(extension)
        }
    }

    fun cancelInstallUpdateExtension(extension: Extension) {
        extensionManager.cancelInstallUpdateExtension(extension)
    }

    private suspend fun Flow<InstallStep>.collectToInstallUpdate(extension: Extension) =
        this
            .firstOrNull { it.isCompleted() }

    fun uninstallExtension(extension: Extension) {
        extensionManager.uninstallExtension(extension)
    }

    fun findAvailableExtensions() {
        screenModelScope.launchIO {
            mutableState.update { it.copy(isRefreshing = true) }

            extensionManager.findAvailableExtensions(forceRefresh = true)

            // Fake slower refresh so it doesn't seem like it's not doing anything
            delay(1.seconds)

            mutableState.update { it.copy(isRefreshing = false) }
        }
    }

    fun trustExtension(extension: Extension.Untrusted) {
        screenModelScope.launch {
            extensionManager.trust(extension)
        }
    }
}
