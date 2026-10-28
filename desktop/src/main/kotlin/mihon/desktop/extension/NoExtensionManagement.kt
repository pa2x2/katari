package mihon.desktop.extension

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.ExtensionManagement
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Desktop loads no extensions yet and lists none from the stores, so the extensions screen has nothing to show, install
 * or uninstall, and entries don't offer to install a missing source's. Stands in until desktop hosts extensions.
 */
@Inject
class NoExtensionManagement : ExtensionManagement {
    override val isInstallOffered: Boolean = false

    override val loaded: Flow<List<Extension.Loaded>> = flowOf(emptyList())

    override val notLoaded: Flow<List<Extension.NotLoaded>> = flowOf(emptyList())

    override val available: Flow<List<Extension.Available>> = flowOf(emptyList())

    override val installSteps: Flow<Map<String, InstallStep>> = flowOf(emptyMap())

    override suspend fun installed(): List<Extension.Installed> = emptyList()

    override fun installed(pkgName: String): Extension.Installed? = null

    override fun listingOf(sourceId: Long): Flow<Extension.Available?> = flowOf(null)

    override suspend fun refreshAvailable(refreshStores: Boolean) = Unit

    override fun install(listing: Extension.Available): Flow<InstallStep> = emptyFlow()

    override fun update(extension: Extension.Installed): Flow<InstallStep> = emptyFlow()

    override fun reinstall(extension: Extension.Installed): Flow<InstallStep> = emptyFlow()

    override fun cancelInstall(extension: Extension) = Unit

    override fun uninstall(extension: Extension.Installed) = Unit

    override fun trust(extension: Extension.NotLoaded) = Unit

    override fun chooseSourceProvider(pkgName: String, sourceKey: Long) = Unit

    override suspend fun clearData(extension: Extension.Loaded) = Unit
}
