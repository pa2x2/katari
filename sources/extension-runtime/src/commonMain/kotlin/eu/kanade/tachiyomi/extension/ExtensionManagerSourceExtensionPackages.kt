package eu.kanade.tachiyomi.extension

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

/** The packages of the extensions the extension manager has loaded. */
@Inject
@ContributesBinding(AppScope::class)
class ExtensionManagerSourceExtensionPackages(
    private val extensionManager: ExtensionManager,
) : SourceExtensionPackages {
    override fun packageName(sourceId: Long): String? = extensionManager.getExtensionPackage(sourceId)

    override fun packageNameChanges(sourceId: Long): Flow<String?> =
        extensionManager.getExtensionPackageAsFlow(sourceId)

    override suspend fun sourceIds(packageName: String): Set<Long> = extensionManager.getLoadedExtensions()
        .filter { it.pkgName == packageName }
        .flatMap { it.sources }
        .map { it.id }
        .toSet()
}
