package eu.kanade.tachiyomi.extension.file

import android.content.Context
import eu.kanade.tachiyomi.extension.kind.ExtensionKind
import eu.kanade.tachiyomi.extension.kind.LoadPassSourceKeys
import eu.kanade.tachiyomi.source.entry.UnifiedSource

/**
 * An ecosystem whose extensions are files the app downloads from a store and runs itself, such as scripts. Nobody signs
 * them: the store they came from vouches for them, and the file installed is pinned by its hash.
 */
internal interface FileExtensionKind : ExtensionKind {

    /**
     * Its installs' folder in [FileExtensionStorage], and the first part of their package names (`directoryName:id`).
     * Lowercase letters only, so it can never be an Android package name.
     */
    val directoryName: String

    /** Orders two of its version names, as stores list them and extensions declare them. */
    fun compareVersions(first: String, second: String): Int

    /**
     * Instantiates the sources of [extension], an installed extension whose file was checked against its hash. A kind
     * whose IDs aren't its sources' keys takes them from [keys]. Throws with what went wrong when it can't.
     */
    suspend fun createSources(
        context: Context,
        extension: InstalledFileExtension,
        keys: LoadPassSourceKeys,
    ): List<UnifiedSource>
}
