package eu.kanade.tachiyomi.ui.browse.extension.details

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.domain.extension.interactor.GetExtensionSources
import eu.kanade.domain.source.interactor.ToggleIncognito
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstalledForm
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.toast
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
import mihon.app.di.AppUiScope
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.catalogue.EntryCatalogueSourceResolution
import mihon.entry.interactions.source.EntrySourceHomeFeature
import mihon.entry.interactions.source.EntrySourceHomeResolution
import mihon.entry.interactions.source.EntrySourceSettingsFeature
import mihon.entry.interactions.source.EntrySourceSettingsResolution
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import okhttp3.HttpUrl.Companion.toHttpUrl
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import kotlin.time.Duration.Companion.seconds

@AssistedInject
class ExtensionDetailsViewModel(
    @Assisted pkgName: String,
    @Assisted private val context: Context,
    private val network: NetworkHelper,
    private val extensionManager: ExtensionManager,
    private val getExtensionSources: GetExtensionSources,
    private val toggleSource: ToggleSource,
    private val toggleIncognito: ToggleIncognito,
    private val preferences: SourcePreferences,
    private val catalogueFeature: EntryCatalogueFeature,
    private val sourceHomeFeature: EntrySourceHomeFeature,
    private val sourceSettingsFeature: EntrySourceSettingsFeature,
    private val profileManager: ProfileManager,
    private val sourcePreferenceFiles: SourcePreferenceFiles,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(pkgName: String, context: Context): ExtensionDetailsViewModel
    }

    val state: StateFlow<ExtensionDetailsState> = extensionManager.loadedExtensionsFlow
        .map { extensions -> extensions.firstOrNull { extension -> extension.pkgName == pkgName } }
        .distinctUntilChanged()
        .flatMapLatest { extension ->
            if (extension == null) return@flatMapLatest flowOf(ExtensionDetailsState.Uninstalled)
            combine(
                subscribeToSources(extension),
                preferences.incognitoExtensions.changes().map { pkgName in it }.distinctUntilChanged(),
                profileManager.activeProfile.map { it?.name.orEmpty() }.distinctUntilChanged(),
            ) { sources, isIncognito, profileName ->
                ExtensionDetailsState.Success(
                    extension = extension,
                    isIncognito = isIncognito,
                    sources = sources,
                    website = website(extension),
                    profileName = profileName,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), ExtensionDetailsState.Loading)

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

    private fun website(extension: Extension.Loaded): String? {
        if (extension.form !is InstalledForm.File) return null
        return extension.sources.firstNotNullOfOrNull { source ->
            (sourceHomeFeature.resolve(source.id) as? EntrySourceHomeResolution.Available)?.url
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

    /** Deletes the settings of every source of the extension in the active profile, logins and saved data included. */
    fun clearData() {
        val extension = loadedExtension ?: return
        viewModelScope.launchIO {
            val profileId = profileManager.activeProfileId
            extension.sources.forEach { source ->
                sourcePreferenceFiles.clear(profileId, sourcePreferenceFiles.sourceIdOf(source.id))
            }
            extensionManager.reloadSettings(extension)
            withUIContext { context.toast(context.stringResource(MR.strings.extension_data_cleared)) }
        }
    }

    fun uninstallExtension() {
        val extension = loadedExtension ?: return
        extensionManager.uninstallExtension(extension)
    }

    fun useForSource(sourceKey: Long) {
        loadedExtension?.pkgName?.let { packageName -> extensionManager.chooseSourceProvider(packageName, sourceKey) }
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
