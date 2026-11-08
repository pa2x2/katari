package eu.kanade.tachiyomi.extension

import app.katari.profiles.core.ProfileStore
import app.katari.profiles.sourcepreferences.SourcePreferenceFiles
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import eu.kanade.domain.source.service.GlobalSourcePreferences
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import kotlinx.coroutines.flow.Flow
import tachiyomi.core.common.preference.getAndSet

/** The apks and files the extension manager loads, installs and lists from the stores. */
@Inject
@ContributesBinding(AppScope::class)
class DefaultExtensionManagement(
    private val extensionManager: ExtensionManager,
    private val profileStore: ProfileStore,
    private val sourcePreferenceFiles: SourcePreferenceFiles,
    private val globalPreferences: GlobalSourcePreferences,
) : ExtensionManagement {
    override val isInstallOffered: Boolean = true

    override val loaded: Flow<List<Extension.Loaded>> = extensionManager.loadedExtensionsFlow

    override val notLoaded: Flow<List<Extension.NotLoaded>> = extensionManager.notLoadedExtensionsFlow

    override val available: Flow<List<Extension.Available>> = extensionManager.availableExtensionsFlow

    override val installSteps: Flow<Map<String, InstallStep>>
        get() = extensionManager.installSteps()

    override suspend fun installed(): List<Extension.Installed> =
        extensionManager.getLoadedExtensions() + extensionManager.getNotLoadedExtensions()

    override fun installed(pkgName: String): Extension.Installed? = extensionManager.getInstalledExtension(pkgName)

    override val listingsBySource: Flow<Map<Long, Extension.Available>> = extensionManager.listingsBySource

    override suspend fun refreshAvailable(refreshStores: Boolean) {
        extensionManager.findAvailableExtensions(forceRefresh = refreshStores)
    }

    override suspend fun fetchListingsOnce() {
        extensionManager.fetchListingsOnce()
    }

    override fun install(listing: Extension.Available): Flow<InstallStep> = extensionManager.installExtension(listing)

    override fun update(extension: Extension.Installed): Flow<InstallStep> = extensionManager.updateExtension(extension)

    override fun reinstall(extension: Extension.Installed): Flow<InstallStep> =
        extensionManager.reinstallExtension(extension)

    override fun cancelInstall(extension: Extension) {
        extensionManager.cancelInstallUpdateExtension(extension)
    }

    override fun uninstall(extension: Extension.Installed) {
        // Uninstalled on purpose, so the Library doesn't suggest installing it again for entries left from it
        globalPreferences.suggestedMissingExtensions.getAndSet { it + extension.pkgName }
        extensionManager.uninstallExtension(extension)
    }

    override fun trust(extension: Extension.NotLoaded) {
        extensionManager.trust(extension)
    }

    override fun chooseSourceProvider(pkgName: String, sourceKey: Long) {
        extensionManager.chooseSourceProvider(pkgName, sourceKey)
    }

    override suspend fun clearData(extension: Extension.Loaded) {
        val profileId = profileStore.currentProfileId
        extension.sources.forEach { source ->
            sourcePreferenceFiles.clear(profileId, sourcePreferenceFiles.sourceIdOf(source.id))
        }
        extensionManager.reloadSettings(extension)
    }
}
