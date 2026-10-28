package eu.kanade.tachiyomi.extension

import kotlinx.coroutines.flow.Flow

/**
 * Which installed extension provides each source, for what is chosen per extension rather than per source: incognito
 * mode for an extension's sources, and a global search over one extension's sources.
 */
interface SourceExtensionPackages {
    /** The package of the installed extension that provides [sourceId], or null for one no extension provides. */
    fun packageName(sourceId: Long): String?

    /** [packageName] of [sourceId], and again each time the installed extensions change. */
    fun packageNameChanges(sourceId: Long): Flow<String?>

    /** The ids of the sources the installed extension [packageName] provides; empty when it isn't installed. */
    suspend fun sourceIds(packageName: String): Set<Long>
}
