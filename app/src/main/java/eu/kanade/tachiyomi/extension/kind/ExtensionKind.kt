package eu.kanade.tachiyomi.extension.kind

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.domain.extension.model.ContentWarning
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceProviderKind

/**
 * One extension ecosystem's rules for turning an installed package into sources. The loader does what every package
 * needs, whichever ecosystem built it: finding installed and private packages, choosing between them, signatures,
 * trust, content warnings and reusing an unchanged extension. A kind adds what its ecosystem decides: the manifest it
 * reads, the extension libraries it runs, its class loader and how its classes become sources.
 *
 * Kinds sharing a [feature] split its packages by extension library; see [ExtensionKinds].
 */
internal interface ExtensionKind {

    /** The `uses-feature` name a package declares to be an extension of this kind. */
    val feature: String

    /** The ID scheme of the IDs its sources were given by their authors. */
    val sourceIdScheme: SourceIdScheme

    val providerKind: SourceProviderKind

    /** The library versions it runs, as `app/extension-compatibility.json` lists them. */
    val family: ExtensionFamily

    /** Whether it runs extensions built against [libVersionName], as a manifest or a store listing declares it. */
    fun runsLib(libVersionName: String): Boolean

    /**
     * Whether [libVersionName] is a version of its library, whether or not it runs that version. Kinds sharing a
     * [feature] tell apart by it which one a package they all refuse was built for.
     */
    fun isBuiltFor(libVersionName: String): Boolean

    /** Whether [libVersionName], a version it doesn't run, is newer than all it does; `null` when it can't be read. */
    fun isNewerThanRun(libVersionName: String): Boolean?

    /** What the manifest says, as this kind reads it. [label] is the application label, for a missing name. */
    fun describe(packageName: String, versionName: String, metaData: Bundle, label: () -> String): ExtensionDescription

    /**
     * Instantiates the extension's sources. A kind whose IDs aren't its sources' keys takes them from [keys]. Throws with
     * what went wrong when it can't.
     */
    suspend fun createSources(
        context: Context,
        appInfo: ApplicationInfo,
        packageName: String,
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
