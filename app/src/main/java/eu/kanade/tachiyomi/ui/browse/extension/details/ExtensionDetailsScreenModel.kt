package eu.kanade.tachiyomi.ui.browse.extension.details

import android.content.Context
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.extension.interactor.GetExtensionSources
import eu.kanade.domain.source.interactor.ToggleIncognito
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import logcat.LogPriority
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.catalogue.EntryCatalogueSourceResolution
import mihon.entry.interactions.source.EntrySourceHomeFeature
import mihon.entry.interactions.source.EntrySourceHomeResolution
import mihon.entry.interactions.source.EntrySourceSettingsFeature
import mihon.entry.interactions.source.EntrySourceSettingsResolution
import okhttp3.HttpUrl.Companion.toHttpUrl
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

class ExtensionDetailsScreenModel(
    pkgName: String,
    private val context: Context,
    private val network: NetworkHelper = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val getExtensionSources: GetExtensionSources = Injekt.get(),
    private val toggleSource: ToggleSource = Injekt.get(),
    private val toggleIncognito: ToggleIncognito = Injekt.get(),
    private val preferences: SourcePreferences = Injekt.get(),
    private val catalogueFeature: EntryCatalogueFeature = Injekt.get(),
    private val sourceHomeFeature: EntrySourceHomeFeature = Injekt.get(),
    private val sourceSettingsFeature: EntrySourceSettingsFeature = Injekt.get(),
) : ScreenModel {

    val state: StateFlow<ExtensionDetailsState> = extensionManager.loadedExtensionsFlow
        .map { extensions -> extensions.firstOrNull { extension -> extension.pkgName == pkgName } }
        .distinctUntilChanged()
        .flatMapLatest { extension ->
            if (extension == null) return@flatMapLatest flowOf(ExtensionDetailsState.Uninstalled)
            combine(
                subscribeToSources(extension),
                preferences.incognitoExtensions.changes().map { pkgName in it }.distinctUntilChanged(),
            ) { sources, isIncognito ->
                ExtensionDetailsState.Success(extension = extension, isIncognito = isIncognito, sources = sources)
            }
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5.seconds), ExtensionDetailsState.Loading)

    private val loadedExtension: Extension.Loaded?
        get() = (state.value as? ExtensionDetailsState.Success)?.extension

    private fun subscribeToSources(
        extension: Extension.Loaded,
    ): Flow<ImmutableList<ExtensionDetailsSourceUiModel>> {
        return getExtensionSources.subscribe(extension)
            .map { sources ->
                sources
                    .sortedWith(
                        compareBy(
                            { !it.enabled },
                            { item ->
                                item.source.name.takeIf { item.labelAsName }
                                    ?: LocaleHelper.getSourceDisplayName(
                                        catalogueSource(item.source.id)?.language.orEmpty(),
                                        context,
                                    ).lowercase()
                            },
                        ),
                    )
                    .map { source ->
                        val catalogueSource = catalogueSource(source.source.id)
                        ExtensionDetailsSourceUiModel(
                            id = source.source.id,
                            name = source.source.name,
                            title = source.source.name,
                            lang = catalogueSource?.language.orEmpty(),
                            labelAsName = source.labelAsName,
                            enabled = source.enabled,
                            hasSettings = sourceSettingsFeature.resolve(source.source.id) is
                                EntrySourceSettingsResolution.Available,
                            supportedEntryTypes = catalogueSource?.supportedEntryTypes,
                        )
                    }
                    .toImmutableList()
            }
            .catch { throwable ->
                logcat(LogPriority.ERROR, throwable)
                emit(persistentListOf())
            }
    }

    private fun catalogueSource(sourceId: Long) =
        (catalogueFeature.source(sourceId) as? EntryCatalogueSourceResolution.Available)?.source

    fun clearCookies() {
        val extension = loadedExtension ?: return

        val urls = extension.sources
            .mapNotNull { source ->
                (sourceHomeFeature.resolve(source.id) as? EntrySourceHomeResolution.Available)?.url
            }
            .distinct()

        val cleared = urls.sumOf {
            try {
                network.cookieJar.remove(it.toHttpUrl())
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to clear cookies for $it" }
                0
            }
        }

        logcat { "Cleared $cleared cookies for: ${urls.joinToString()}" }
    }

    fun uninstallExtension() {
        val extension = loadedExtension ?: return
        extensionManager.uninstallExtension(extension)
    }

    fun toggleSource(sourceId: Long) {
        toggleSource.await(sourceId)
    }

    fun toggleSources(enable: Boolean) {
        loadedExtension?.sources
            ?.map { it.id }
            ?.let { toggleSource.await(it, enable) }
    }

    fun toggleIncognito(enable: Boolean) {
        loadedExtension?.pkgName?.let { packageName ->
            toggleIncognito.await(packageName, enable)
        }
    }
}
