package eu.kanade.tachiyomi.extension.kind

import app.katari.domain.extension.model.ContentWarning
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.platform.ExtensionClassLoaders
import eu.kanade.tachiyomi.extension.platform.ExtensionMetaData
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import eu.kanade.tachiyomi.source.entry.UnifiedSource

/**
 * An ecosystem whose extensions are apks. It adds the manifest it reads, its class loader and how its classes become
 * sources; the loader finds installed and private packages, chooses between them and checks their signatures.
 *
 * Kinds sharing a [feature] split its packages by extension library; see [ExtensionKinds].
 */
internal interface PackageExtensionKind : ExtensionKind {

    /** The `uses-feature` name a package declares to be an extension of this kind. */
    val feature: String

    /** What the manifest says, as this kind reads it. [label] is the application label, for a missing name. */
    fun describe(
        packageName: String,
        versionName: String,
        metaData: ExtensionMetaData,
        label: () -> String,
    ): ExtensionDescription

    /**
     * Instantiates the extension's sources, loading its classes through [classLoaders]. A kind whose IDs aren't its
     * sources' keys takes them from [keys]. Throws with what went wrong when it can't.
     */
    suspend fun createSources(
        extensionPackage: ExtensionPackage,
        classLoaders: ExtensionClassLoaders,
        description: ExtensionDescription,
        keys: LoadPassSourceKeys,
    ): List<UnifiedSource>
}

internal data class ExtensionDescription(
    val name: String,
    val contentWarning: ContentWarning,
    /** The extension library it declares, `null` when neither the manifest nor the version name says. */
    val libVersionName: String?,
    val libVersion: Double?,
    /** Fully qualified, in declaration order; empty when the manifest names none. */
    val sourceClassNames: List<String>,
    val factory: String?,
    /** Why this kind won't load the package whatever the user allows, `null` when it would. */
    val refusal: Extension.NotLoaded.Reason?,
)
