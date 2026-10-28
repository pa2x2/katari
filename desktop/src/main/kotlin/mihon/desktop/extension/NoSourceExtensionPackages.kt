package mihon.desktop.extension

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.SourceExtensionPackages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Desktop loads no extensions yet, so no source comes from one: no extension's incognito setting applies to it, and a
 * search over one extension's sources finds none. Stands in until desktop hosts extensions.
 */
@Inject
class NoSourceExtensionPackages : SourceExtensionPackages {
    override fun packageName(sourceId: Long): String? = null

    override fun packageNameChanges(sourceId: Long): Flow<String?> = flowOf(null)

    override suspend fun sourceIds(packageName: String): Set<Long> = emptySet()
}
