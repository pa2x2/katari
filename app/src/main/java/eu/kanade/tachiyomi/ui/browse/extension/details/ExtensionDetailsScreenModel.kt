package eu.kanade.tachiyomi.ui.browse.extension.details

import android.content.Context
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.extension.interactor.GetExtensionSources
import eu.kanade.domain.source.interactor.ToggleIncognito
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.EntryCatalogueFeature
import mihon.entry.interactions.EntryCatalogueSourceResolution
import mihon.entry.interactions.EntrySourceHomeFeature
import mihon.entry.interactions.EntrySourceHomeResolution
import mihon.entry.interactions.EntrySourceSettingsFeature
import mihon.entry.interactions.EntrySourceSettingsResolution
import okhttp3.HttpUrl.Companion.toHttpUrl
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionDetailsScreenModel(
    pkgName: String,
    context: Context,
    private val network: NetworkHelper = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val getExtensionSources: GetExtensionSources = Injekt.get(),
    private val toggleSource: ToggleSource = Injekt.get(),
    private val toggleIncognito: ToggleIncognito = Injekt.get(),
    private val preferences: SourcePreferences = Injekt.get(),
    private val catalogueFeature: EntryCatalogueFeature = Injekt.get(),
    private val sourceHomeFeature: EntrySourceHomeFeature = Injekt.get(),
    private val sourceSettingsFeature: EntrySourceSettingsFeature = Injekt.get(),
) : StateScreenModel<ExtensionDetailsState>(ExtensionDetailsState()) {

    private val _events: Channel<ExtensionDetailsEvent> = Channel()
    val events: Flow<ExtensionDetailsEvent> = _events.receiveAsFlow()

    init {
        screenModelScope.launch {
            launch {
                extensionManager.installedExtensionsFlow
                    .map { extensions ->
                        extensions.firstOrNull { extension -> extension.pkgName == pkgName }
                    }
                    .collectLatest { extension ->
                        if (extension == null) {
                            _events.send(ExtensionDetailsEvent.Uninstalled)
                            return@collectLatest
                        }
                        mutableState.update { state ->
                            state.copy(extension = extension)
                        }
                    }
            }
            launch {
                state.collectLatest { state ->
                    val extension = state.extension ?: return@collectLatest
                    getExtensionSources.subscribe(extension)
                        .map {
                            it.sortedWith(
                                compareBy(
                                    { !it.enabled },
                                    { item ->
                                        val source = (
                                            catalogueFeature.source(item.source.id) as?
                                                EntryCatalogueSourceResolution.Available
                                            )?.source
                                        item.source.name.takeIf { item.labelAsName }
                                            ?: LocaleHelper.getSourceDisplayName(
                                                source?.language.orEmpty(),
                                                context,
                                            ).lowercase()
                                    },
                                ),
                            )
                        }
                        .catch { throwable ->
                            logcat(LogPriority.ERROR, throwable)
                            mutableState.update { it.copyWithSources(persistentListOf()) }
                        }
                        .collectLatest { sources ->
                            mutableState.update {
                                it.copyWithSources(
                                    sources
                                        .map { source ->
                                            val catalogueSource = (
                                                catalogueFeature.source(source.source.id) as?
                                                    EntryCatalogueSourceResolution.Available
                                                )?.source
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
                                        .toImmutableList(),
                                )
                            }
                        }
                }
            }
            launch {
                preferences.incognitoExtensions
                    .changes()
                    .map { pkgName in it }
                    .distinctUntilChanged()
                    .collectLatest { isIncognito ->
                        mutableState.update { it.copy(isIncognito = isIncognito) }
                    }
            }
        }
    }

    fun clearCookies() {
        val extension = state.value.extension ?: return

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
        val extension = state.value.extension ?: return
        extensionManager.uninstallExtension(extension)
    }

    fun toggleSource(sourceId: Long) {
        toggleSource.await(sourceId)
    }

    fun toggleSources(enable: Boolean) {
        state.value.extension?.sources
            ?.map { it.id }
            ?.let { toggleSource.await(it, enable) }
    }

    fun toggleIncognito(enable: Boolean) {
        state.value.extension?.pkgName?.let { packageName ->
            toggleIncognito.await(packageName, enable)
        }
    }
}

sealed interface ExtensionDetailsEvent {
    data object Uninstalled : ExtensionDetailsEvent
}
