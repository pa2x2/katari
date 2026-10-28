package eu.kanade.tachiyomi.extension

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles

/** The apks and files the extension manager loads, installs and lists from the stores. */
@Inject
class DefaultExtensionManagement(
    private val extensionManager: ExtensionManager,
    private val profileStore: ProfileStore,
    private val sourcePreferenceFiles: SourcePreferenceFiles,
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

    override fun listingOf(sourceId: Long): Flow<Extension.Available?> =
        extensionManager.availableExtensionsFlow.map { extensionManager.getListingOfSource(sourceId) }

    override suspend fun refreshAvailable(refreshStores: Boolean) {
        extensionManager.findAvailableExtensions(forceRefresh = refreshStores)
    }

    override fun install(listing: Extension.Available): Flow<InstallStep> = extensionManager.installExtension(listing)

    override fun update(extension: Extension.Installed): Flow<InstallStep> = extensionManager.updateExtension(extension)

    override fun reinstall(extension: Extension.Installed): Flow<InstallStep> =
        extensionManager.reinstallExtension(extension)

    override fun cancelInstall(extension: Extension) {
        extensionManager.cancelInstallUpdateExtension(extension)
    }

    override fun uninstall(extension: Extension.Installed) {
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
